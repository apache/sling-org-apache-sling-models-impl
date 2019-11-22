/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.models.impl;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletRequestWrapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.ValidationStrategy;
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.annotations.via.BeanProperty;
import org.apache.sling.models.export.spi.ModelExporter;
import org.apache.sling.models.factory.ExportException;
import org.apache.sling.models.factory.InvalidAdaptableException;
import org.apache.sling.models.factory.InvalidModelException;
import org.apache.sling.models.factory.MissingElementException;
import org.apache.sling.models.factory.MissingElementsException;
import org.apache.sling.models.factory.MissingExporterException;
import org.apache.sling.models.factory.ModelClassException;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.models.factory.PostConstructException;
import org.apache.sling.models.factory.ValidationException;
import org.apache.sling.models.impl.injectors.OSGiServiceInjector;
import org.apache.sling.models.impl.model.ConstructorParameter;
import org.apache.sling.models.impl.model.InjectableElement;
import org.apache.sling.models.impl.model.InjectableField;
import org.apache.sling.models.impl.model.InjectableMethod;
import org.apache.sling.models.impl.model.ModelClass;
import org.apache.sling.models.impl.model.ModelClassConstructor;
import org.apache.sling.models.spi.AcceptsNullName;
import org.apache.sling.models.spi.DisposalCallback;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.ImplementationPicker;
import org.apache.sling.models.spi.Injector;
import org.apache.sling.models.spi.ModelValidation;
import org.apache.sling.models.spi.ValuePreparer;
import org.apache.sling.models.spi.ViaProvider;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessorFactory2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.apache.sling.scripting.api.BindingsValuesProvidersByContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(immediate = true, service={ ModelFactory.class, ServletRequestListener.class }, 
    property= { HttpWhiteboardConstants.HTTP_WHITEBOARD_LISTENER+"=true", 
                HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT+"=(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)" })
@Designate(ocd=ModelAdapterFactoryConfiguration.class)
@SuppressWarnings("deprecation")
public class ModelAdapterFactory implements AdapterFactory, Runnable, ModelFactory, ServletRequestListener {

    // hard code this value since we always know exactly how many there are
    private static final int VALUE_PREPARERS_COUNT = 2;

    private static final String REQUEST_MARKER_ATTRIBUTE = ModelAdapterFactory.class.getName() + ".RealRequest";

    private static final Object REQUEST_MARKER_VALUE = new Object();

    private static class DisposalCallbackRegistryImpl implements DisposalCallbackRegistry, Disposable {

        private List<DisposalCallback> callbacks = new ArrayList<>();

        @Override
        public void addDisposalCallback(@NotNull DisposalCallback callback) {
            callbacks.add(callback);
        }

        private void seal() {
            callbacks = Collections.unmodifiableList(callbacks);
        }


        @Override
        public void onDisposed() {
            for (DisposalCallback callback : callbacks) {
                callback.onDisposed();
            }
        }

    }

    private static class CombinedDisposable implements Disposable {
        private final Collection<Disposable> delegates = Collections.synchronizedCollection(new HashSet<Disposable>());

        private void add(Disposable disposable) {
            delegates.add(disposable);
        }

        @Override
        public void onDisposed() {
            for (Disposable delegate : delegates) {
                delegate.onDisposed();
            }
        }
    }

    private interface Disposable  {
        void onDisposed();
    }

    private static class RequestDisposalCallbacks {
        private ConcurrentHashMap<ServletRequest, Disposable> callbacks = new ConcurrentHashMap<>();

        public Collection<Disposable> values() {
            return callbacks.values();
        }

        public Disposable remove(ServletRequest request) {
            return callbacks.remove(request);
        }

        public void put(ServletRequest request, Disposable registry) {
            synchronized (callbacks) {
                CombinedDisposable combinedDisposable = null;
                Disposable current = callbacks.get(request);
                if (current == null) {
                    callbacks.put(request, registry);
                    return;
                } else if (current instanceof CombinedDisposable) {
                    combinedDisposable = (CombinedDisposable) current;
                } else {
                    combinedDisposable = new CombinedDisposable();
                    combinedDisposable.add(current);
                    callbacks.put(request, combinedDisposable);
                }
                combinedDisposable.add(registry);
            }
        }
    }

    private ReferenceQueue<Object> queue;

    private ConcurrentMap<java.lang.ref.Reference<Object>, Disposable> disposalCallbacks;

    private RequestDisposalCallbacks requestDisposalCallbacks;

    @Override
    public void run() {
        clearDisposalCallbackRegistryQueue();
    }

    private void clearDisposalCallbackRegistryQueue() {
        java.lang.ref.Reference<?> ref = queue.poll();
        while (ref != null) {
            log.debug("calling disposal for {}.", ref);
            Disposable registry = disposalCallbacks.remove(ref);
            registry.onDisposed();
            ref = queue.poll();
        }
    }

    private static final Logger log = LoggerFactory.getLogger(ModelAdapterFactory.class);

    private final @NotNull ConcurrentMap<String, RankedServices<Injector>> injectors = new ConcurrentHashMap<>();
    private final @NotNull RankedServices<Injector> sortedInjectors = new RankedServices<>();
    private final @NotNull ConcurrentMap<Class<? extends ViaProviderType>, ViaProvider> viaProviders = new ConcurrentHashMap<>();

    @Reference(name="injectAnnotationProcessorFactory", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    volatile @NotNull Collection<InjectAnnotationProcessorFactory> injectAnnotationProcessorFactories; // this must be non-final for fieldOption=replace!

    @Reference(name="injectAnnotationProcessorFactory2", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    volatile @NotNull Collection<InjectAnnotationProcessorFactory2> injectAnnotationProcessorFactories2; // this must be non-final for fieldOption=replace!

    private final @NotNull RankedServices<StaticInjectAnnotationProcessorFactory> staticInjectAnnotationProcessorFactories = new RankedServices<>();

    private final @NotNull RankedServices<ImplementationPicker> implementationPickers = new RankedServices<>();

    // bind the service with the highest priority (if a new one comes in this service gets restarted)
    @Reference(cardinality=ReferenceCardinality.OPTIONAL, policyOption=ReferencePolicyOption.GREEDY)
    private ModelValidation modelValidation = null;

    @Reference(name = "modelExporter", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    volatile @NotNull Collection<ModelExporter> modelExporters; // this must be non-final for fieldOption=replace!

    @Reference
    BindingsValuesProvidersByContext bindingsValuesProvidersByContext;

    @Reference 
    AdapterManager adapterManager;

    ModelPackageBundleListener listener;

    final AdapterImplementations adapterImplementations = new AdapterImplementations();

    private ServiceRegistration jobRegistration;

    private ServiceRegistration configPrinterRegistration;

    // Use threadlocal to count recursive invocations and break recursing if a max. limit is reached (to avoid cyclic dependencies)
    private ThreadLocal<ThreadInvocationCounter> invocationCountThreadLocal;

    private Map<Object, Map<Class<?>, SoftReference<Object>>> adapterCache;

    private SlingModelsScriptEngineFactory scriptEngineFactory;

    // use a smaller initial capacity than the default as we expect a relatively small number of
    // adapters per adaptable
    private final int INNER_CACHE_INITIAL_CAPACITY = 4;


    @Override
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        Result<AdapterType> result = internalCreateModel(adaptable, type);
        if (!result.wasSuccessful()) {
            log.warn("Could not adapt to model", result.getThrowable());
            return null;
        } else {
            return result.getValue();
        }
    }

    @Override
    public @NotNull <ModelType> ModelType createModel(@NotNull Object adaptable, @NotNull Class<ModelType> type) throws MissingElementsException,
            InvalidAdaptableException, ValidationException, InvalidModelException {
        Result<ModelType> result = internalCreateModel(adaptable, type);
        if (!result.wasSuccessful()) {
            throw result.getThrowable();
        }
        return result.getValue();
    }

    @Override
    public boolean canCreateFromAdaptable(@NotNull Object adaptable, @NotNull Class<?> modelClass) throws ModelClassException {
        return internalCanCreateFromAdaptable(adaptable, modelClass);
    }

    private boolean internalCanCreateFromAdaptable(Object adaptable, Class<?> requestedType) throws ModelClassException {
        try {
            ModelClass<?> modelClass = getImplementationTypeForAdapterType(requestedType, adaptable);
            Class<?>[] declaredAdaptable = modelClass.getModelAnnotation().adaptables();
            for (Class<?> clazz : declaredAdaptable) {
                if (clazz.isInstance(adaptable)) {
                    return true;
                }
            }
        } catch (ModelClassException e) {
            log.debug("Could not find implementation for given type " + requestedType + ". Probably forgot either the model annotation or it was not registered as adapter factory (yet)", e);
            return false;
        }
        return false;
    }

    @Override
    @Deprecated
    public boolean isModelClass(@NotNull Object adaptable, @NotNull Class<?> requestedType) {
        try {
            getImplementationTypeForAdapterType(requestedType, adaptable);
        } catch (ModelClassException e) {
            log.debug("Could not find implementation for given adaptable. Probably forgot either the model annotation or it was not registered as adapter factory (yet)", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean isModelClass(@NotNull Class<?> type) {
        return this.adapterImplementations.isModelClass(type);
    }

    /**
     *
     * @param requestedType the adapter type
     * @param adaptable the adaptable
     * @return the implementation type to use for the desired model type or null if there is none registered
     * @see <a
     *      href="http://sling.apache.org/documentation/bundles/models.html#specifying-an-alternate-adapter-class-since-sling-models-110">Specifying
     *      an Alternate Adapter Class</a>
     */
    private <ModelType> ModelClass<ModelType> getImplementationTypeForAdapterType(Class<ModelType> requestedType, Object adaptable) {
        // lookup ModelClass wrapper for implementation type
        // additionally check if a different implementation class was registered for this adapter type
        // the adapter implementation is initially filled by the ModelPackageBundleList
        ModelClass<ModelType> modelClass = this.adapterImplementations.lookup(requestedType, adaptable);
        if (modelClass != null) {
            log.debug("Using implementation type {} for requested adapter type {}", modelClass, requestedType);
            return modelClass;
        }
        // throw exception here
        throw new ModelClassException("Could not yet find an adapter factory for the model " + requestedType + " from adaptable " + adaptable.getClass());
    }

    @SuppressWarnings("unchecked")
    private <ModelType> Result<ModelType> internalCreateModel(final Object adaptable, final Class<ModelType> requestedType) {
        Result<ModelType> result;
        ThreadInvocationCounter threadInvocationCounter = invocationCountThreadLocal.get();
        if (threadInvocationCounter.isMaximumReached()) {
            String msg = String.format("Adapting %s to %s failed, too much recursive invocations (>=%s).",
                    adaptable, requestedType, threadInvocationCounter.maxRecursionDepth);
            return new Result<>(new ModelClassException(msg));
        }
        threadInvocationCounter.increase();
        try {
            // check if a different implementation class was registered for this adapter type
            ModelClass<ModelType> modelClass = getImplementationTypeForAdapterType(requestedType, adaptable);

            if (!modelClass.hasModelAnnotation()) {
                String msg = String.format("Provided Adapter class does not have a Model annotation: %s", modelClass.getType());
                return new Result<>(new ModelClassException(msg));
            }
            boolean isAdaptable = false;

            Model modelAnnotation = modelClass.getModelAnnotation();

            if (modelAnnotation.cache()) {
                Map<Class<?>, SoftReference<Object>> adaptableCache = adapterCache.get(adaptable);
                if (adaptableCache != null) {
                    SoftReference<Object> softReference = adaptableCache.get(requestedType);
                    if (softReference != null) {
                        ModelType cachedObject = (ModelType) softReference.get();
                        if (cachedObject != null) {
                            return new Result<>(cachedObject);
                        }
                    }
                }
            }

            Class<?>[] declaredAdaptable = modelAnnotation.adaptables();
            for (Class<?> clazz : declaredAdaptable) {
                if (clazz.isInstance(adaptable)) {
                    isAdaptable = true;
                }
            }
            if (!isAdaptable) {
                String msg = String.format("Adaptables (%s) are not acceptable for the model class: %s", StringUtils.join(declaredAdaptable), modelClass.getType());
                return new Result<>(new InvalidAdaptableException(msg));
            } else {
                RuntimeException t = validateModel(adaptable, modelClass.getType(), modelAnnotation);
                if (t != null) {
                    return new Result<>(t);
                }
                if (modelClass.getType().isInterface()) {
                    Result<InvocationHandler> handlerResult = createInvocationHandler(adaptable, modelClass);
                    if (handlerResult.wasSuccessful()) {
                        ModelType model = (ModelType) Proxy.newProxyInstance(modelClass.getType().getClassLoader(), new Class<?>[] { modelClass.getType() }, handlerResult.getValue());

                        if (modelAnnotation.cache()) {
                            Map<Class<?>, SoftReference<Object>> adaptableCache = adapterCache.get(adaptable);
                            if (adaptableCache == null) {
                                adaptableCache = Collections.synchronizedMap(new WeakHashMap<Class<?>, SoftReference<Object>>());
                                adapterCache.put(adaptable, adaptableCache);
                            }
                            adaptableCache.put(requestedType, new SoftReference<Object>(model));
                        }

                        result = new Result<>(model);
                    } else {
                        return new Result<>(handlerResult.getThrowable());
                    }
                } else {
                    try {
                        result = createObject(adaptable, modelClass);

                        if (result.wasSuccessful() && modelAnnotation.cache()) {
                            Map<Class<?>, SoftReference<Object>> adaptableCache = adapterCache.get(adaptable);
                            if (adaptableCache == null) {
                                adaptableCache = Collections.synchronizedMap(new WeakHashMap<Class<?>, SoftReference<Object>>());
                                adapterCache.put(adaptable, adaptableCache);
                            }
                            adaptableCache.put(requestedType, new SoftReference<Object>(result.getValue()));
                        }
                    } catch (Exception e) {
                        String msg = String.format("Unable to create model %s", modelClass.getType());
                        return new Result<>(new ModelClassException(msg, e));
                    }
                }
            }
            return result;
        } finally {
            threadInvocationCounter.decrease();
        }
    }

    private <ModelType> RuntimeException validateModel(Object adaptable, Class<ModelType> modelType, Model modelAnnotation) {
        if (modelAnnotation.validation() != ValidationStrategy.DISABLED) {
            if (modelValidation == null) {
                return new ValidationException("No active service for ModelValidation found, therefore no validation can be performed.");
            }
            return modelValidation.validate(adaptable, modelType, modelAnnotation.validation() == ValidationStrategy.REQUIRED);
        }
        return null;
    }

    private interface InjectCallback {
        /**
         * Is called each time when the given value should be injected into the given element
         * @param element
         * @param value
         * @return an InjectionResult
         */
        RuntimeException inject(InjectableElement element, Object value);
    }

    private class SetFieldCallback implements InjectCallback {

        private final Object object;

        private SetFieldCallback(Object object) {
            this.object = object;
        }

        @Override
        public RuntimeException inject(InjectableElement element, Object value) {
            return setField((InjectableField) element, object, value);
        }
    }

    private class SetMethodsCallback implements InjectCallback {

        private final Map<Method, Object> methods;

        private SetMethodsCallback( Map<Method, Object> methods) {
            this.methods = methods;
        }

        @Override
        public RuntimeException inject(InjectableElement element, Object value) {
            return setMethod((InjectableMethod) element, methods, value);
        }
    }

    private class SetConstructorParameterCallback implements InjectCallback {

        private final List<Object> parameterValues;

        private SetConstructorParameterCallback(List<Object> parameterValues) {
            this.parameterValues = parameterValues;
        }

        @Override
        public RuntimeException inject(InjectableElement element, Object value) {
            return setConstructorParameter((ConstructorParameter)element, parameterValues, value);
        }
    }

    private
    @Nullable
    RuntimeException injectElement(final InjectableElement element, final Object adaptable,
                                   final @NotNull DisposalCallbackRegistry registry, final InjectCallback callback,
                                   final @NotNull Map<ValuePreparer, Object> preparedValues,
                                   final @Nullable BundleContext modelContext) {

        InjectAnnotationProcessor annotationProcessor = null;
        String source = element.getSource();
        boolean wasInjectionSuccessful = false;

        // find an appropriate annotation processor
        for (InjectAnnotationProcessorFactory2 factory : injectAnnotationProcessorFactories2) {
            annotationProcessor = factory.createAnnotationProcessor(adaptable, element.getAnnotatedElement());
            if (annotationProcessor != null) {
                break;
            }
        }
        if (annotationProcessor == null) {
            for (InjectAnnotationProcessorFactory factory : injectAnnotationProcessorFactories) {
                annotationProcessor = factory.createAnnotationProcessor(adaptable, element.getAnnotatedElement());
                if (annotationProcessor != null) {
                    break;
                }
            }
        }

        String name = getName(element, annotationProcessor);
        final Object injectionAdaptable = getAdaptable(adaptable, element, annotationProcessor);

        RuntimeException lastInjectionException = null;
        if (injectionAdaptable != null) {

            // prepare the set of injectors to process. if a source is given only use injectors with this name.
            final RankedServices<Injector> injectorsToProcess;
            if (StringUtils.isEmpty(source)) {
                injectorsToProcess = sortedInjectors;
            }
            else {
                injectorsToProcess = injectors.get(source);
                if (injectorsToProcess == null) {
                    throw new IllegalArgumentException("No Sling Models Injector registered for source '" + source + "'.");
                }
            }

            // find the right injector
            for (Injector injector : injectorsToProcess) {
                if (name != null || injector instanceof AcceptsNullName) {
                    Object preparedValue = injectionAdaptable;

                    // only do the ValuePreparer optimization for the original adaptable
                    if (injector instanceof ValuePreparer && adaptable == injectionAdaptable) {
                        final ValuePreparer preparer = (ValuePreparer) injector;
                        Object fromMap = preparedValues.get(preparer);
                        if (fromMap != null) {
                            preparedValue = fromMap;
                        } else {
                            preparedValue = preparer.prepareValue(injectionAdaptable);
                            preparedValues.put(preparer, preparedValue);
                        }
                    }
                    Object value = null;
                    if (injector instanceof OSGiServiceInjector) {
                        value = ((OSGiServiceInjector)injector).getValue(preparedValue, name, element.getType(), element.getAnnotatedElement(), registry, modelContext);
                    } else {
                        value = injector.getValue(preparedValue, name, element.getType(), element.getAnnotatedElement(), registry);
                    }
                    if (value != null) {
                        lastInjectionException = callback.inject(element, value);
                        if (lastInjectionException == null) {
                            wasInjectionSuccessful = true;
                            break;
                        }
                    }
                }
            }
        }
        // if injection failed, use default
        if (!wasInjectionSuccessful) {
            Result<Boolean> defaultInjectionResult = injectDefaultValue(element, annotationProcessor, callback);
            if (defaultInjectionResult.wasSuccessful()) {
                wasInjectionSuccessful = defaultInjectionResult.getValue();
                // log previous injection error, if there was any
                if (lastInjectionException != null && wasInjectionSuccessful) {
                    log.debug("Although falling back to default value worked, injection into {} failed because of: " + lastInjectionException.getMessage(), element.getAnnotatedElement(), lastInjectionException);
                }
            } else {
                return defaultInjectionResult.getThrowable();
            }
        }

        // if default is not set, check if mandatory
        if (!wasInjectionSuccessful) {
            if (element.isOptional(annotationProcessor)) {
                // log previous injection error, if there was any
                if (lastInjectionException != null) {
                    log.debug("Injection into optional element {} failed because of: " + lastInjectionException.getMessage(), element.getAnnotatedElement(), lastInjectionException);
                }
                if (element.isPrimitive()) {
                    RuntimeException throwable = injectPrimitiveInitialValue(element, callback);
                    if (throwable != null) {
                        return throwable;
                    }
                }
            } else {
                if (lastInjectionException != null) {
                    return lastInjectionException;
                } else {
                    return new ModelClassException("No injector returned a non-null value!");
                }
            }
        }
        return null;
    }

    private BundleContext getModelBundleContext(final ModelClass<?> modelClass) {
        BundleContext modelContext = null;
        Bundle modelBundle = FrameworkUtil.getBundle(modelClass.getType());
        if (modelBundle != null) {
            return modelBundle.getBundleContext();
        }
        return null;
    }

    private <ModelType> Result<InvocationHandler> createInvocationHandler(final Object adaptable, final ModelClass<ModelType> modelClass) {
        InjectableMethod[] injectableMethods = modelClass.getInjectableMethods();
        final Map<Method, Object> methods = new HashMap<>();
        SetMethodsCallback callback = new SetMethodsCallback(methods);
        MapBackedInvocationHandler handler = new MapBackedInvocationHandler(methods);

        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();

        final Map<ValuePreparer, Object> preparedValues = new HashMap<>(VALUE_PREPARERS_COUNT);
        List<MissingElementException> missingElements = null;
        final BundleContext modelContext = getModelBundleContext(modelClass);
        for (InjectableMethod method : injectableMethods) {
            RuntimeException t = injectElement(method, adaptable, registry, callback, preparedValues, modelContext);
            if (t != null) {
                if (missingElements == null) {
                    missingElements = new ArrayList<>();
                }
                missingElements.add(new MissingElementException(method.getAnnotatedElement(), t));
            }
        }

        if (!registry.callbacks.isEmpty()) {
            registry.seal();

            if (adaptable instanceof SlingHttpServletRequest &&
                    ((SlingHttpServletRequest) adaptable).getAttribute(REQUEST_MARKER_ATTRIBUTE) == REQUEST_MARKER_VALUE) {
                registerRequestCallbackRegistry((SlingHttpServletRequest) adaptable, registry);
            } else {
                registerCallbackRegistry(handler, registry);
            }
        }
        if (missingElements != null) {
            MissingElementsException missingElementsException = new MissingElementsException("Could not create all mandatory methods for interface of model " + modelClass);
            for (MissingElementException me : missingElements) {
                missingElementsException.addMissingElementExceptions(me);
            }
            return new Result<>(missingElementsException);
        }
        return new Result<InvocationHandler>(handler);
    }

    private void registerCallbackRegistry(Object object, DisposalCallbackRegistryImpl registry) {
        PhantomReference<Object> reference = new PhantomReference<>(object, queue);
        disposalCallbacks.put(reference, registry);
    }

    private void registerRequestCallbackRegistry(ServletRequest request, DisposalCallbackRegistryImpl registry) {
        request = unwrapRequest(request);
        requestDisposalCallbacks.put(request, registry);
    }

    private static ServletRequest unwrapRequest(ServletRequest request) {
        while (request instanceof ServletRequestWrapper) {
            request = ((ServletRequestWrapper) request).getRequest();
        }
        return request;
    }

    private <ModelType> Result<ModelType> createObject(final Object adaptable, final ModelClass<ModelType> modelClass)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();

        ModelClassConstructor<ModelType> constructorToUse = getBestMatchingConstructor(adaptable, modelClass);
        if (constructorToUse == null) {
            return new Result<>(new ModelClassException("Unable to find a useable constructor for model " + modelClass.getType()));
        }

        final Map<ValuePreparer, Object> preparedValues = new HashMap<>(VALUE_PREPARERS_COUNT);

        ModelType object;
        if (constructorToUse.getConstructor().getParameterTypes().length == 0) {
            // no parameters for constructor injection? instantiate it right away
            object = constructorToUse.getConstructor().newInstance();
        } else {
            // instantiate with constructor injection
            // if this fails, make sure resources that may be claimed by injectors are cleared up again
            try {
                Result<ModelType> result = newInstanceWithConstructorInjection(constructorToUse, adaptable, modelClass, registry, preparedValues);
                if (!result.wasSuccessful()) {
                    registry.onDisposed();
                    return result;
                } else {
                    object = result.getValue();
                }
            } catch (InstantiationException ex) {
                registry.onDisposed();
                throw ex;
            } catch (InvocationTargetException ex) {
                registry.onDisposed();
                throw ex;
            } catch (IllegalAccessException ex) {
                registry.onDisposed();
                throw ex;
            }
        }

        InjectCallback callback = new SetFieldCallback(object);

        InjectableField[] injectableFields = modelClass.getInjectableFields();
        List<MissingElementException> missingElements = null;
        final BundleContext modelContext = getModelBundleContext(modelClass);
        for (InjectableField field : injectableFields) {
            RuntimeException t = injectElement(field, adaptable, registry, callback, preparedValues, modelContext);
            if (t != null) {
                if (missingElements == null) {
                    missingElements = new ArrayList<>();
                }
                missingElements.add(new MissingElementException(field.getAnnotatedElement(), t));
            }
        }

        if (!registry.callbacks.isEmpty()) {
            registry.seal();

            if (adaptable instanceof SlingHttpServletRequest &&
                    ((SlingHttpServletRequest) adaptable).getAttribute(REQUEST_MARKER_ATTRIBUTE) == REQUEST_MARKER_VALUE) {
                registerRequestCallbackRegistry((SlingHttpServletRequest) adaptable, registry);
            } else {
                registerCallbackRegistry(object, registry);
            }

        }
        if (missingElements != null) {
            MissingElementsException missingElementsException = new MissingElementsException("Could not inject all required fields into " + modelClass.getType());
            for (MissingElementException me : missingElements) {
                missingElementsException.addMissingElementExceptions(me);
            }
            return new Result<>(missingElementsException);
        }
        try {
            object = invokePostConstruct(object);
        } catch (InvocationTargetException e) {
            return new Result<>(new PostConstructException("Post-construct method has thrown an exception for model " + modelClass.getType(), e.getCause()));
        } catch (IllegalAccessException e) {
            new Result<ModelType>(new ModelClassException("Could not call post-construct method for model " + modelClass.getType(), e));
        }
        return new Result<>(object);
    }

    /**
     * Gets best matching constructor for constructor injection - or default constructor if none is found.
     * @param adaptable Adaptable instance
     * @param type Model type
     * @return Constructor or null if none found
     */
    @SuppressWarnings("unchecked")
    private <ModelType> ModelClassConstructor<ModelType> getBestMatchingConstructor(Object adaptable, ModelClass<ModelType> type) {
        ModelClassConstructor<ModelType>[] constructors = type.getConstructors();

        for (ModelClassConstructor<ModelType> constructor : constructors) {
            // first try to find the constructor with most parameters and @Inject annotation
            if (constructor.hasInjectAnnotation()) {
                return constructor;
            }
            // compatibility mode for sling models implementation <= 1.0.6:
            // support constructor without @Inject if it has exactly one parameter matching the adaptable class
            final Class<?>[] paramTypes = constructor.getConstructor().getParameterTypes();
            if (paramTypes.length == 1) {
                Class<?> paramType = constructor.getConstructor().getParameterTypes()[0];
                if (paramType.isInstance(adaptable)) {
                    return constructor;
                }
            }
            // if no constructor for injection found use public constructor without any params
            if (constructor.getConstructor().getParameterTypes().length == 0) {
                return constructor;
            }
        }
        return null;
    }

    private <ModelType> Result<ModelType> newInstanceWithConstructorInjection(final ModelClassConstructor<ModelType> constructor, final Object adaptable,
            final ModelClass<ModelType> modelClass, final DisposalCallbackRegistry registry, final @NotNull Map<ValuePreparer, Object> preparedValues)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        ConstructorParameter[] parameters = constructor.getConstructorParameters();

        List<Object> paramValues = new ArrayList<>(Arrays.asList(new Object[parameters.length]));
        InjectCallback callback = new SetConstructorParameterCallback(paramValues);

        final BundleContext modelContext = getModelBundleContext(modelClass);
        List<MissingElementException> missingElements = null;
        for (int i = 0; i < parameters.length; i++) {
            RuntimeException t = injectElement(parameters[i], adaptable, registry, callback, preparedValues, modelContext);
            if (t != null) {
                if (missingElements == null) {
                    missingElements = new ArrayList<>();
                }
                missingElements.add(new MissingElementException(parameters[i].getAnnotatedElement(), t));
            }
        }
        if (missingElements != null) {
            MissingElementsException missingElementsException = new MissingElementsException("Required constructor parameters were not able to be injected on model " + modelClass.getType());
            for (MissingElementException me : missingElements) {
                missingElementsException.addMissingElementExceptions(me);
            }
            return new Result<>(missingElementsException);
        }
        return new Result<>(constructor.getConstructor().newInstance(paramValues.toArray(new Object[paramValues.size()])));
    }

    private Result<Boolean> injectDefaultValue(InjectableElement point, InjectAnnotationProcessor processor,
            InjectCallback callback) {
        if (processor != null) {
            if (processor.hasDefault()) {
                RuntimeException t = callback.inject(point, processor.getDefault());
                if (t == null) {
                    return new Result<>(Boolean.TRUE);
                } else {
                    return new Result<>(t);
                }
            }
        }

        Object value = point.getDefaultValue();
        if (value != null) {
            RuntimeException t = callback.inject(point, value);
            if (t == null) {
                return new Result<>(Boolean.TRUE);
            } else {
                return new Result<>(t);
            }
        }
        else {
            return new Result<>(Boolean.FALSE);
        }
    }

    /**
     * Injects the default initial value for the given primitive class which
     * cannot be null (e.g. int = 0, boolean = false).
     *
     * @param point Annotated element
     * @param callback Inject callback
     */
    private RuntimeException injectPrimitiveInitialValue(InjectableElement point, InjectCallback callback) {
        Type primitiveType = ReflectionUtil.mapWrapperClasses(point.getType());
        Object value = null;
        if (primitiveType == int.class) {
            value = 0;
        } else if (primitiveType == long.class) {
            value = 0L;
        } else if (primitiveType == boolean.class) {
            value = Boolean.FALSE;
        } else if (primitiveType == double.class) {
            value = 0.0d;
        } else if (primitiveType == float.class) {
            value = 0.0f;
        } else if (primitiveType == short.class) {
            value = (short) 0;
        } else if (primitiveType == byte.class) {
            value = (byte) 0;
        } else if (primitiveType == char.class) {
            value = '\u0000';
        }
        if (value != null) {
            return callback.inject(point, value);
        } else {
            return new ModelClassException(String.format("Unknown primitive type %s", primitiveType.toString()));
        }
    }

    private Object getAdaptable(Object adaptable, InjectableElement point, InjectAnnotationProcessor processor) {
        String viaValue = null;
        Class<? extends ViaProviderType> viaProviderType = null;
        if (processor != null) {
            viaValue = processor.getVia();
            viaProviderType = BeanProperty.class; // processors don't support via provider type
        }
        if (StringUtils.isBlank(viaValue)) {
            viaValue = point.getVia();
            viaProviderType = point.getViaProviderType();
        }
        if (viaProviderType == null || viaValue == null) {
            return adaptable;
        }
        ViaProvider viaProvider = viaProviders.get(viaProviderType);
        if (viaProvider == null) {
            log.error("Unable to find Via provider type {}.", viaProviderType);
            return null;
        }
        final Object viaResult = viaProvider.getAdaptable(adaptable, viaValue);
        if (viaResult == ViaProvider.ORIGINAL) {
            return adaptable;
        } else {
            return viaResult;
        }
    }

    private String getName(InjectableElement element, InjectAnnotationProcessor processor) {
        // try to get the name from injector-specific annotation
        if (processor != null) {
            String name = processor.getName();
            if (name != null) {
                return name;
            }
        }
        // get name from @Named annotation or element name
        return element.getName();
    }

    private boolean addMethodIfNotOverriden(List<Method> methods, Method newMethod) {
        for (Method method : methods) {
            if (method.getName().equals(newMethod.getName())) {
                if (Arrays.equals(method.getParameterTypes(),newMethod.getParameterTypes())) {
                    return false;
                }
            }
        }
        methods.add(newMethod);
        return true;
    }

    private <ModelType> ModelType invokePostConstruct(ModelType object) throws InvocationTargetException, IllegalAccessException {
        Class<?> clazz = object.getClass();
        List<Method> postConstructMethods = new ArrayList<>();
        while (clazz != null) {
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    addMethodIfNotOverriden(postConstructMethods, method);
                }
            }
            clazz = clazz.getSuperclass();
        }
        Collections.reverse(postConstructMethods);
        for (Method method : postConstructMethods) {
            boolean accessible = method.isAccessible();
            try {
                if (!accessible) {
                    method.setAccessible(true);
                }
                Object result = method.invoke(object);
                if (result instanceof Boolean && !((Boolean) result).booleanValue()) {
                    log.debug("PostConstruct method {}.{} returned false. Returning null model.", method.getDeclaringClass().getName(), method.getName());
                    return null;
                }
            } finally {
                if (!accessible) {
                    method.setAccessible(false);
                }
            }
        }
        return object;
    }

    private RuntimeException setField(InjectableField injectableField, Object createdObject, Object value) {
        Result<Object> result = adaptIfNecessary(value, injectableField.getFieldType(), injectableField.getFieldGenericType());
        if (result.wasSuccessful()) {
            return injectableField.set(createdObject, result);
        } else {
            return result.getThrowable();
        }
    }

    private RuntimeException setMethod(InjectableMethod injectableMethod, Map<Method, Object> methods, Object value) {
        Method method = injectableMethod.getMethod();
        Result<Object> result = adaptIfNecessary(value, method.getReturnType(), method.getGenericReturnType());
        if (result.wasSuccessful()) {
            methods.put(method, result.getValue());
            return null;
        } else {
            return result.getThrowable();
        }
    }

    private RuntimeException setConstructorParameter(ConstructorParameter constructorParameter, List<Object> parameterValues, Object value) {
        if (constructorParameter.getParameterType() instanceof Class<?>) {
            Result<Object> result = adaptIfNecessary(value, (Class<?>) constructorParameter.getParameterType(), constructorParameter.getGenericType());
            if (result.wasSuccessful() ) {
                parameterValues.set(constructorParameter.getParameterIndex(), result.getValue());
                return null;
            } else {
                return result.getThrowable();
            }
        } else {
            return new ModelClassException(String.format("Constructor parameter with index %d is not a class!", constructorParameter.getParameterIndex()));
        }
    }

    private Result<Object> adaptIfNecessary(final Object value, final Class<?> type, final Type genericType) {
        final Object adaptedValue;
        if (!isAcceptableType(type, genericType, value)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                if (value instanceof Collection &&
                        (type.equals(Collection.class) || type.equals(List.class)) &&
                        parameterizedType.getActualTypeArguments().length == 1) {

                    List<Object> result = new ArrayList<>();
                    for (Object valueObject : (Collection<?>) value) {
                        Result<Object> singleValueResult = adapt(valueObject, (Class<?>) parameterizedType.getActualTypeArguments()[0], true);
                        if (singleValueResult.wasSuccessful()) {
                            result.add(singleValueResult.getValue());
                        } else {
                            return singleValueResult;
                        }
                    }
                    adaptedValue = result;
                } else {
                    return new Result<>(new ModelClassException(String.format("%s is neither a parameterized Collection or List",
                        type)));
                }
            } else {
                return adapt(value, type, false);
            }
            return new Result<>(adaptedValue);
        } else {
            return new Result<>(value);
        }
    }

    /**
     * Preferably adapt via the {@link ModelFactory} in case the target type is a Sling Model itself, otherwise use regular {@link Adaptable#adaptTo(Class)}.
     * @param value the object from which to adapt
     * @param type the target type
     * @param isWithinCollection
     * @return a Result either encapsulating an exception or the adapted value
     */
    private @Nullable Result<Object> adapt(final Object value, final Class<?> type, boolean isWithinCollection) {
        Object adaptedValue = null;
        final String messageSuffix = isWithinCollection ? " in collection" : "";
        if (isModelClass(type) && canCreateFromAdaptable(value, type)) {
            Result<?> result = internalCreateModel(value, type);
            if (result.wasSuccessful()) {
                adaptedValue = result.getValue();
            } else {
                return new Result<>(new ModelClassException(
                    String.format("Could not create model from %s: %s%s", value.getClass(), result.getThrowable().getMessage(), messageSuffix),
                    result.getThrowable()));
            }
        } else if (value instanceof Adaptable) {
            adaptedValue = ((Adaptable) value).adaptTo(type);
            if (adaptedValue == null) {
                return new Result<>(new ModelClassException(String.format("Could not adapt from %s to %s%s", value.getClass(), type, messageSuffix)));
            }
        }
        if (adaptedValue != null) {
            return new Result<>(adaptedValue);
        } else {
            return new Result<>(new ModelClassException(
                    String.format("Could not adapt from %s to %s%s, because this class is not adaptable!", value.getClass(), type, messageSuffix)));
        }
    }

    private static boolean isAcceptableType(Class<?> type, Type genericType, Object value) {
        if (type.isInstance(value)) {
            if ((type == Collection.class || type == List.class) && genericType instanceof ParameterizedType &&
                    value instanceof Collection) {
                Iterator<?> it = ((Collection<?>) value).iterator();
                if (!it.hasNext()) {
                    // empty collection, so it doesn't really matter
                    return true;
                } else {
                    // this is not an ideal way to get the actual component type, but erasure...
                    Class<?> actualComponentType = it.next().getClass();
                    Class<?> desiredComponentType = (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
                    return desiredComponentType.isAssignableFrom(actualComponentType);
                }
            } else {
                return true;
            }
        }

        if (type == Integer.TYPE) {
            return Integer.class.isInstance(value);
        }
        if (type == Long.TYPE) {
            return Long.class.isInstance(value);
        }
        if (type == Boolean.TYPE) {
            return Boolean.class.isInstance(value);
        }
        if (type == Double.TYPE) {
            return Double.class.isInstance(value);
        }
        if (type == Float.TYPE) {
            return Float.class.isInstance(value);
        }
        if (type == Short.TYPE) {
            return Short.class.isInstance(value);
        }
        if (type == Byte.TYPE) {
            return Byte.class.isInstance(value);
        }
        if (type == Character.TYPE) {
            return Character.class.isInstance(value);
        }

        return false;
    }

    @Activate
    protected void activate(final ComponentContext ctx, final ModelAdapterFactoryConfiguration configuration) {
        this.invocationCountThreadLocal = new ThreadLocal<ThreadInvocationCounter>() {
            @Override
            protected ThreadInvocationCounter initialValue() {
                return new ThreadInvocationCounter(configuration.max_recursion_depth());
            }
        };

        this.adapterCache = Collections.synchronizedMap(new WeakHashMap<Object, Map<Class<?>, SoftReference<Object>>>());

        BundleContext bundleContext = ctx.getBundleContext();
        this.queue = new ReferenceQueue<>();
        this.disposalCallbacks = new ConcurrentHashMap<>();
        this.requestDisposalCallbacks = new RequestDisposalCallbacks();
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        properties.put(Constants.SERVICE_DESCRIPTION, "Sling Models OSGi Service Disposal Job");
        properties.put("scheduler.name", "Sling Models OSGi Service Disposal Job");
        properties.put("scheduler.concurrent", false);
        properties.put("scheduler.period", configuration.cleanup_job_period());

        this.jobRegistration = bundleContext.registerService(Runnable.class, this, properties);

        this.scriptEngineFactory = new SlingModelsScriptEngineFactory(bundleContext.getBundle());
        this.listener = new ModelPackageBundleListener(ctx.getBundleContext(), this, this.adapterImplementations, bindingsValuesProvidersByContext, scriptEngineFactory);

        Hashtable<String, Object> printerProps = new Hashtable<>();
        printerProps.put(Constants.SERVICE_VENDOR, "Apache Software Foundation");
        printerProps.put(Constants.SERVICE_DESCRIPTION, "Sling Models Configuration Printer");
        printerProps.put("felix.webconsole.label", "slingmodels");
        printerProps.put("felix.webconsole.title", "Sling Models");
        printerProps.put("felix.webconsole.configprinter.modes", "always");

        this.configPrinterRegistration = bundleContext.registerService(Object.class,
                new ModelConfigurationPrinter(this, bundleContext, adapterImplementations), printerProps);

    }

    @Deactivate
    protected void deactivate() {
        this.adapterCache = null;
        if (this.requestDisposalCallbacks != null) {
            for (final Disposable requestRegistries : this.requestDisposalCallbacks.values()) {
                requestRegistries.onDisposed();
            }
        }
        this.requestDisposalCallbacks = null;
        this.clearDisposalCallbackRegistryQueue();
        this.listener.unregisterAll();
        this.adapterImplementations.removeAll();
        if (jobRegistration != null) {
            jobRegistration.unregister();
            jobRegistration = null;
        }
        if (configPrinterRegistration != null) {
            configPrinterRegistration.unregister();
            configPrinterRegistration = null;
        }
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindInjector(final Injector injector, final Map<String, Object> props) {
        RankedServices<Injector> newRankedServices = new RankedServices<>();
        RankedServices<Injector> injectorsPerInjectorName = injectors.putIfAbsent(injector.getName(), newRankedServices);
        if (injectorsPerInjectorName == null) {
            injectorsPerInjectorName = newRankedServices;
        }
        injectorsPerInjectorName.bind(injector, props);
        sortedInjectors.bind(injector, props);
    }

    protected void unbindInjector(final Injector injector, final Map<String, Object> props) {
        RankedServices<Injector> injectorsPerInjectorName = injectors.get(injector.getName());
        if (injectorsPerInjectorName != null) {
            injectorsPerInjectorName.unbind(injector, props);
        }
        sortedInjectors.unbind(injector, props);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindStaticInjectAnnotationProcessorFactory(final StaticInjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (staticInjectAnnotationProcessorFactories) {
            staticInjectAnnotationProcessorFactories.bind(factory, props);
            this.adapterImplementations.setStaticInjectAnnotationProcessorFactories(staticInjectAnnotationProcessorFactories.get());
        }
    }

    protected void unbindStaticInjectAnnotationProcessorFactory(final StaticInjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (staticInjectAnnotationProcessorFactories) {
            staticInjectAnnotationProcessorFactories.unbind(factory, props);
            this.adapterImplementations.setStaticInjectAnnotationProcessorFactories(staticInjectAnnotationProcessorFactories.get());
        }
    }

    @Reference(name="implementationPicker", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindImplementationPicker(final ImplementationPicker implementationPicker, final Map<String, Object> props) {
        synchronized (implementationPickers) {
            implementationPickers.bind(implementationPicker, props);
            this.adapterImplementations.setImplementationPickers(implementationPickers.get());
        }
    }

    protected void unbindImplementationPicker(final ImplementationPicker implementationPicker, final Map<String, Object> props) {
        synchronized (implementationPickers) {
            implementationPickers.unbind(implementationPicker, props);
            this.adapterImplementations.setImplementationPickers(implementationPickers.get());
        }
    }

    @Reference(
            name = "viaProvider",
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC
            )
    protected void bindViaProvider(final ViaProvider viaProvider, final Map<String, Object> props) {
        Class<? extends ViaProviderType> type = viaProvider.getType();
        viaProviders.put(type, viaProvider);
    }

    protected void unbindViaProvider(final ViaProvider viaProvider, final Map<String, Object> props) {
        Class<? extends ViaProviderType> type = viaProvider.getType();
        viaProviders.remove(type, viaProvider);
    }

    @NotNull Collection<Injector> getInjectors() {
        return sortedInjectors.get();
    }

    @NotNull Collection<InjectAnnotationProcessorFactory> getInjectAnnotationProcessorFactories() {
        return injectAnnotationProcessorFactories;
    }

    @NotNull Collection<InjectAnnotationProcessorFactory2> getInjectAnnotationProcessorFactories2() {
        return injectAnnotationProcessorFactories2;
    }

    @NotNull Collection<StaticInjectAnnotationProcessorFactory> getStaticInjectAnnotationProcessorFactories() {
        return staticInjectAnnotationProcessorFactories.get();
    }

    @NotNull ImplementationPicker[] getImplementationPickers() {
        return adapterImplementations.getImplementationPickers();
    }

    @NotNull Map<Class<? extends ViaProviderType>, ViaProvider> getViaProviders() {
        return viaProviders;
    }

    @Override
    public boolean isModelAvailableForRequest(@NotNull SlingHttpServletRequest request) {
        return adapterImplementations.getModelClassForRequest(request) != null;
    }

    @Override
    public boolean isModelAvailableForResource(@NotNull Resource resource) {
        return adapterImplementations.getModelClassForResource(resource) != null;
    }

    @Override
    public Object getModelFromResource(Resource resource) {
        Class<?> clazz = this.adapterImplementations.getModelClassForResource(resource);
        if (clazz == null) {
            throw new ModelClassException("Could find model registered for resource type: " + resource.getResourceType());
        }
        return handleBoundModelResult(internalCreateModel(resource, clazz));
    }

    @Override
    public Object getModelFromRequest(SlingHttpServletRequest request) {
        Class<?> clazz = this.adapterImplementations.getModelClassForRequest(request);
        if (clazz == null) {
            throw new ModelClassException("Could find model registered for request path: " + request.getServletPath());
        }
        return handleBoundModelResult(internalCreateModel(request, clazz));
    }

    private Object handleBoundModelResult(Result<?> result) {
        if (!result.wasSuccessful()) {
            throw result.getThrowable();
        } else {
            return result.getValue();
        }
    }

    @Override
    public <T> T exportModel(Object model, String name, Class<T> targetClass, Map<String, String> options)
            throws ExportException, MissingExporterException {
        for (ModelExporter exporter : modelExporters) {
            if (exporter.getName().equals(name) && exporter.isSupported(targetClass)) {
                T resultObject = exporter.export(model, targetClass, options);
                return resultObject;
            }
        }
        throw new MissingExporterException(name, targetClass);
    }

    @Override
    public <T> T exportModelForResource(Resource resource, String name, Class<T> targetClass, Map<String, String> options)
            throws ExportException, MissingExporterException {
        Class<?> clazz = this.adapterImplementations.getModelClassForResource(resource);
        if (clazz == null) {
            throw new ModelClassException("Could find model registered for resource type: " + resource.getResourceType());
        }
        Result<?> result = internalCreateModel(resource, clazz);
        return handleAndExportResult(result, name, targetClass, options);
    }

    @Override
    public <T> T exportModelForRequest(SlingHttpServletRequest request, String name, Class<T> targetClass, Map<String, String> options)
            throws ExportException, MissingExporterException {
        Class<?> clazz = this.adapterImplementations.getModelClassForRequest(request);
        if (clazz == null) {
            throw new ModelClassException("Could find model registered for request path: " + request.getServletPath());
        }
        Result<?> result = internalCreateModel(request, clazz);
        return handleAndExportResult(result, name, targetClass, options);
    }

    protected <T> T handleAndExportResult(Result<?> result, String name, Class<T> targetClass, Map<String, String> options) throws ExportException, MissingExporterException {
        if (result.wasSuccessful()) {
            return exportModel(result.getValue(), name, targetClass, options);
        } else {
            throw result.getThrowable();
        }
    }

    @Override
    public <T> T getModelFromWrappedRequest(@NotNull SlingHttpServletRequest request, @NotNull Resource resource, @NotNull Class<T> targetClass) {
        return new ResourceOverridingRequestWrapper(request, resource, adapterManager,
                scriptEngineFactory, bindingsValuesProvidersByContext).adaptTo(targetClass);
    }

    @Override
    public void requestDestroyed(ServletRequestEvent sre) {
        ServletRequest request = unwrapRequest(sre.getServletRequest());
        Disposable registry = requestDisposalCallbacks.remove(request);
        if (registry != null) {
            registry.onDisposed();
        }
    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {
        sre.getServletRequest().setAttribute(REQUEST_MARKER_ATTRIBUTE, REQUEST_MARKER_VALUE);
    }
}
