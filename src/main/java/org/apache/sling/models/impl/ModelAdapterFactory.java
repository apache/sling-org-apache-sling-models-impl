/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.models.impl;

import javax.annotation.PostConstruct;
import javax.servlet.ServletRequest;

import java.io.PrintWriter;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
import org.apache.sling.models.impl.model.OptionalTypedInjectableElement;
import org.apache.sling.models.spi.AcceptsNullName;
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
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.FieldOption;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        immediate = true,
        service = {ModelFactory.class},
        property = {
            "felix.webconsole.label=slingmodels",
            "felix.webconsole.title=Sling Models",
            "felix.webconsole.configprinter.modes=always",
            "scheduler.name=Sling Models OSGi Service Disposal Job",
            "scheduler.concurrent=false"
        })
@Designate(ocd = ModelAdapterFactoryConfiguration.class)
@SuppressWarnings("deprecation")
public class ModelAdapterFactory implements ModelFactory, Runnable {

    // hard code this value since we always know exactly how many there are
    private static final int VALUE_PREPARERS_COUNT = 2;

    private static final String REQUEST_CACHE_ATTRIBUTE = ModelAdapterFactory.class.getName() + ".AdapterCache";

    private final Logger log = LoggerFactory.getLogger(ModelAdapterFactory.class);

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    /** Injectors are sorted by DS according to their service ranking */
    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            fieldOption = FieldOption.REPLACE)
    volatile List<Injector> injectors;

    private final ConcurrentMap<Class<? extends ViaProviderType>, ViaProvider> viaProviders = new ConcurrentHashMap<>();

    @Reference(
            name = "injectAnnotationProcessorFactory",
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    volatile Collection<InjectAnnotationProcessorFactory>
            injectAnnotationProcessorFactories; // this must be non-final for fieldOption=replace!

    @Reference(
            name = "injectAnnotationProcessorFactory2",
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    volatile Collection<InjectAnnotationProcessorFactory2>
            injectAnnotationProcessorFactories2; // this must be non-final for fieldOption=replace!

    private volatile Map<Comparable<?>, StaticInjectAnnotationProcessorFactory>
            staticInjectAnnotationProcessorFactories = Collections.emptyMap();

    /** Implementation pickers are sorted by DS according to their service ranking */
    @Reference(
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC,
            fieldOption = FieldOption.REPLACE)
    volatile List<ImplementationPicker> implementationPickers;

    // bind the service with the highest priority (if a new one comes in this service gets restarted)
    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policyOption = ReferencePolicyOption.GREEDY)
    private ModelValidation modelValidation;

    @Reference(name = "modelExporter", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    volatile Collection<ModelExporter> modelExporters; // this must be non-final for fieldOption=replace!

    @Reference
    BindingsValuesProvidersByContext bindingsValuesProvidersByContext;

    @Reference
    AdapterManager adapterManager;

    ModelPackageBundleListener listener;

    final AdapterImplementations adapterImplementations = new AdapterImplementations();

    private ModelConfigurationPrinter configPrinter;

    // Use threadlocal to count recursive invocations and break recursing if a max. limit is reached (to avoid cyclic
    // dependencies)
    private ThreadLocal<ThreadInvocationCounter> invocationCountThreadLocal;

    private Map<Object, Map<Class<?>, SoftReference<Object>>> adapterCache;

    private SlingModelsScriptEngineFactory scriptEngineFactory;

    @Override
    public void run() {
        DisposalCallbackRegistryImpl.cleanupDisposables();
    }

    @SuppressWarnings("null")
    public <AdapterType> AdapterType getAdapter(Object adaptable, Class<AdapterType> type) {
        Result<AdapterType> result = internalCreateModel(adaptable, type);
        if (!result.wasSuccessful()) {
            if (result == Result.POST_CONSTRUCT_PREVENTED_MODEL_CONSTRUCTION) {
                log.debug("Could not adapt to model as PostConstruct method returned false"); // do no construct runtime
                // exception in this case
            } else {
                log.warn("Could not adapt to model", result.getThrowable());
            }
            return null;
        } else {
            return result.getValue();
        }
    }

    @Override
    public @NotNull <ModelType> ModelType createModel(@NotNull Object adaptable, @NotNull Class<ModelType> type)
            throws MissingElementsException, InvalidAdaptableException, ValidationException, InvalidModelException {
        Result<ModelType> result = internalCreateModel(adaptable, type);
        if (!result.wasSuccessful()) {
            throw result.getThrowable();
        }
        return result.getValue();
    }

    @Override
    public @NotNull <T> T createModelFromWrappedRequest(
            @NotNull SlingHttpServletRequest request, @NotNull Resource resource, @NotNull Class<T> targetClass) {
        return createModel(
                new ResourceOverridingRequestWrapper(
                        request, resource, adapterManager, scriptEngineFactory, bindingsValuesProvidersByContext),
                targetClass);
    }

    @Override
    public boolean canCreateFromAdaptable(@NotNull Object adaptable, @NotNull Class<?> modelClass)
            throws ModelClassException {
        return internalCanCreateFromAdaptable(adaptable, modelClass);
    }

    private boolean internalCanCreateFromAdaptable(Object adaptable, Class<?> requestedType)
            throws ModelClassException {
        try {
            ModelClass<?> modelClass = getImplementationTypeForAdapterType(requestedType, adaptable);
            Class<?>[] declaredAdaptable = modelClass.getModelAnnotation().adaptables();
            for (Class<?> clazz : declaredAdaptable) {
                if (clazz.isInstance(adaptable)) {
                    return true;
                }
            }
        } catch (ModelClassException e) {
            log.debug(
                    "Could not find implementation for given type " + requestedType
                            + ". Probably forgot either the model annotation or it was not registered as adapter factory (yet)",
                    e);
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
            log.debug(
                    "Could not find implementation for given adaptable. Probably forgot either the model annotation or it was not registered as adapter factory (yet)",
                    e);
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
    private <ModelType> ModelClass<ModelType> getImplementationTypeForAdapterType(
            Class<ModelType> requestedType, Object adaptable) {
        // lookup ModelClass wrapper for implementation type
        // additionally check if a different implementation class was registered for this adapter type
        // the adapter implementation is initially filled by the ModelPackageBundleList
        ModelClass<ModelType> modelClass =
                this.adapterImplementations.lookup(requestedType, adaptable, this.implementationPickers);
        if (modelClass != null) {
            log.debug("Using implementation type {} for requested adapter type {}", modelClass, requestedType);
            return modelClass;
        }
        // throw exception here
        throw new ModelClassException("Could not yet find an adapter factory for the model " + requestedType
                + " from adaptable " + adaptable.getClass());
    }

    @SuppressWarnings("unchecked")
    private Map<Class<?>, SoftReference<Object>> getOrCreateCache(final Object adaptable) {
        Map<Class<?>, SoftReference<Object>> adaptableCache;
        if (adaptable instanceof ServletRequest) {
            ServletRequest request = (ServletRequest) adaptable;
            adaptableCache = (Map<Class<?>, SoftReference<Object>>) request.getAttribute(REQUEST_CACHE_ATTRIBUTE);
            if (adaptableCache == null) {
                adaptableCache = Collections.synchronizedMap(new WeakHashMap<>());
                request.setAttribute(REQUEST_CACHE_ATTRIBUTE, adaptableCache);
            }
        } else {
            adaptableCache =
                    adapterCache.computeIfAbsent(adaptable, k -> Collections.synchronizedMap(new WeakHashMap<>()));
        }
        return adaptableCache;
    }

    @SuppressWarnings("unchecked")
    <ModelType> Result<ModelType> internalCreateModel(final Object adaptable, final Class<ModelType> requestedType) {
        Result<ModelType> result;
        ThreadInvocationCounter threadInvocationCounter = invocationCountThreadLocal.get();
        if (threadInvocationCounter.isMaximumReached()) {
            String msg = String.format(
                    "Adapting %s to %s failed, too much recursive invocations (>=%s).",
                    adaptable, requestedType, threadInvocationCounter.maxRecursionDepth);
            return new Result<>(new ModelClassException(msg));
        }
        threadInvocationCounter.increase();
        try {
            // check if a different implementation class was registered for this adapter type
            ModelClass<ModelType> modelClass = getImplementationTypeForAdapterType(requestedType, adaptable);

            if (!modelClass.hasModelAnnotation()) {
                String msg = String.format(
                        "Provided Adapter class does not have a Model annotation: %s", modelClass.getType());
                return new Result<>(new ModelClassException(msg));
            }
            boolean isAdaptable = false;

            Model modelAnnotation = modelClass.getModelAnnotation();
            Map<Class<?>, SoftReference<Object>> adaptableCache = null;

            if (modelAnnotation.cache()) {
                adaptableCache = getOrCreateCache(adaptable);
                SoftReference<Object> softReference = adaptableCache.get(modelClass.getType());
                if (softReference != null) {
                    ModelType cachedObject = (ModelType) softReference.get();
                    if (cachedObject != null) {
                        return new Result<>(cachedObject);
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
                String msg = String.format(
                        "Given adaptable (%s) is not acceptable for the model class: %s which only supports adaptables %s",
                        adaptable.getClass(), modelClass.getType(), StringUtils.join(declaredAdaptable));
                return new Result<>(new InvalidAdaptableException(msg));
            } else {
                RuntimeException t = validateModel(adaptable, modelClass.getType(), modelAnnotation);
                if (t != null) {
                    return new Result<>(t);
                }
                if (modelClass.getType().isInterface()) {
                    Result<InvocationHandler> handlerResult = createInvocationHandler(adaptable, modelClass);
                    if (handlerResult.wasSuccessful()) {
                        ModelType model = (ModelType) Proxy.newProxyInstance(
                                modelClass.getType().getClassLoader(),
                                new Class<?>[] {modelClass.getType()},
                                handlerResult.getValue());

                        if (modelAnnotation.cache() && adaptableCache != null) {
                            adaptableCache.put(modelClass.getType(), new SoftReference<>(model));
                        }

                        result = new Result<>(model);
                    } else {
                        return new Result<>(handlerResult.getThrowable());
                    }
                } else {
                    try {
                        result = createObject(adaptable, modelClass);

                        if (result.wasSuccessful() && modelAnnotation.cache() && adaptableCache != null) {
                            adaptableCache.put(modelClass.getType(), new SoftReference<>(result.getValue()));
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

    private <ModelType> RuntimeException validateModel(
            Object adaptable, Class<ModelType> modelType, Model modelAnnotation) {
        if (modelAnnotation.validation() != ValidationStrategy.DISABLED) {
            if (modelValidation == null) {
                return new ValidationException(
                        "No active service for ModelValidation found, therefore no validation can be performed.");
            }
            return modelValidation.validate(
                    adaptable, modelType, modelAnnotation.validation() == ValidationStrategy.REQUIRED);
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

        private SetMethodsCallback(Map<Method, Object> methods) {
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
            return setConstructorParameter((ConstructorParameter) element, parameterValues, value);
        }
    }

    private class OptionalWrappingCallback implements InjectCallback {

        private final InjectCallback chainedCallback;
        private final InjectableElement element;

        private OptionalWrappingCallback(InjectCallback chainedCallback, InjectableElement element) {
            this.chainedCallback = chainedCallback;
            this.element = element;
        }

        @Override
        public RuntimeException inject(InjectableElement element1, Object value) {
            return chainedCallback.inject(
                    element,
                    value.equals(Optional.empty())
                            ? value // if the value is null it's already represented as Optional.empty(), return as is
                            : Optional.of(value)); // otherwise wrap in Optional
        }
    }

    private @Nullable RuntimeException injectElement(
            final InjectableElement element,
            final Object adaptable,
            final @NotNull DisposalCallbackRegistry registry,
            final InjectCallback callback,
            final @NotNull Map<ValuePreparer, Object> preparedValues,
            final @Nullable BundleContext modelContext) {
        if (element instanceof InjectableField) {
            Type genericType = ((InjectableField) element).getFieldGenericType();

            if (genericType instanceof ParameterizedType) {
                ParameterizedType pType = (ParameterizedType) genericType;

                if (pType.getRawType().equals(Optional.class)) {
                    InjectableElement el =
                            new OptionalTypedInjectableElement(element, pType.getActualTypeArguments()[0]);
                    InjectCallback wrappedCallback = new OptionalWrappingCallback(callback, element);

                    return injectElementInternal(
                            el, adaptable, registry, wrappedCallback, preparedValues, modelContext);
                }
            }
        }

        return injectElementInternal(element, adaptable, registry, callback, preparedValues, modelContext);
    }

    private @Nullable RuntimeException injectElementInternal(
            final InjectableElement element,
            final Object adaptable,
            final @NotNull DisposalCallbackRegistry registry,
            final InjectCallback callback,
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

            if (StringUtils.isEmpty(source)) {
                source = null;
            }
            // find the right injector
            final List<Injector> localInjectors = this.injectors;
            boolean foundSource = false;
            for (final Injector injector : localInjectors) {
                // if a source is given only use injectors with this name.
                if (source != null && !source.equals(injector.getName())) {
                    continue;
                }
                foundSource = true;
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
                        value = ((OSGiServiceInjector) injector)
                                .getValue(
                                        preparedValue,
                                        name,
                                        element.getType(),
                                        element.getAnnotatedElement(),
                                        registry,
                                        modelContext);
                    } else {
                        value = injector.getValue(
                                preparedValue, name, element.getType(), element.getAnnotatedElement(), registry);
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
            if (!foundSource && source != null) {
                throw new IllegalArgumentException("No Sling Models Injector registered for source '" + source + "'.");
            }
        }
        // if injection failed, use default
        if (!wasInjectionSuccessful) {
            Result<Boolean> defaultInjectionResult = injectDefaultValue(element, annotationProcessor, callback);
            if (defaultInjectionResult.wasSuccessful()) {
                wasInjectionSuccessful = defaultInjectionResult.getValue();
                // log previous injection error, if there was any
                if (lastInjectionException != null && wasInjectionSuccessful) {
                    log.debug(
                            "Although falling back to default value worked, injection into {} failed because of: "
                                    + lastInjectionException.getMessage(),
                            element.getAnnotatedElement(),
                            lastInjectionException);
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
                    log.debug(
                            "Injection into optional element {} failed because of: "
                                    + lastInjectionException.getMessage(),
                            element.getAnnotatedElement(),
                            lastInjectionException);
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
        Bundle modelBundle = FrameworkUtil.getBundle(modelClass.getType());
        if (modelBundle != null) {
            return modelBundle.getBundleContext();
        }
        return null;
    }

    private <ModelType> Result<InvocationHandler> createInvocationHandler(
            final Object adaptable, final ModelClass<ModelType> modelClass) {
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

        registry.registerIfNotEmpty(this.resourceResolverFactory, handler);
        if (missingElements != null) {
            MissingElementsException missingElementsException = new MissingElementsException(
                    "Could not create all mandatory methods for interface of model " + modelClass);
            for (MissingElementException me : missingElements) {
                missingElementsException.addMissingElementExceptions(me);
            }
            return new Result<>(missingElementsException);
        }
        return new Result<InvocationHandler>(handler);
    }

    @SuppressWarnings("unchecked")
    private <ModelType> Result<ModelType> createObject(final Object adaptable, final ModelClass<ModelType> modelClass)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        DisposalCallbackRegistryImpl registry = new DisposalCallbackRegistryImpl();

        ModelClassConstructor<ModelType> constructorToUse = getBestMatchingConstructor(adaptable, modelClass);
        if (constructorToUse == null) {
            return new Result<>(
                    new ModelClassException("Unable to find a useable constructor for model " + modelClass.getType()));
        }

        final Map<ValuePreparer, Object> preparedValues = new HashMap<>(VALUE_PREPARERS_COUNT);

        ModelType object;
        if (constructorToUse.getConstructor().getParameterTypes().length == 0) {
            // no parameters for constructor injection? instantiate it right away

            object = constructorToUse.newInstance();
        } else {
            // instantiate with constructor injection
            // if this fails, make sure resources that may be claimed by injectors are cleared up again
            try {
                Result<ModelType> result = newInstanceWithConstructorInjection(
                        constructorToUse, adaptable, modelClass, registry, preparedValues);
                if (!result.wasSuccessful()) {
                    registry.close();
                    return result;
                } else {
                    object = result.getValue();
                }
            } catch (InstantiationException | InvocationTargetException | IllegalAccessException ex) {
                registry.close();
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

        registry.registerIfNotEmpty(this.resourceResolverFactory, object);
        if (missingElements != null) {
            MissingElementsException missingElementsException =
                    new MissingElementsException("Could not inject all required fields into " + modelClass.getType());
            for (MissingElementException me : missingElements) {
                missingElementsException.addMissingElementExceptions(me);
            }
            return new Result<>(missingElementsException);
        }
        try {
            object = invokePostConstruct(object);
            if (object == null) {
                return (Result<ModelType>) Result.POST_CONSTRUCT_PREVENTED_MODEL_CONSTRUCTION;
            }
        } catch (InvocationTargetException e) {
            return new Result<>(new PostConstructException(
                    "Post-construct method has thrown an exception for model " + modelClass.getType(), e.getCause()));
        } catch (IllegalAccessException e) {
            new Result<ModelType>(new ModelClassException(
                    "Could not call post-construct method for model " + modelClass.getType(), e));
        }
        return new Result<>(object);
    }

    /**
     * Gets best matching constructor for constructor injection - or default constructor if none is found.
     * @param adaptable Adaptable instance
     * @param type Model type
     * @return Constructor or null if none found
     */
    private <ModelType> ModelClassConstructor<ModelType> getBestMatchingConstructor(
            Object adaptable, ModelClass<ModelType> type) {
        ModelClassConstructor<ModelType>[] constructors = type.getConstructors();

        for (ModelClassConstructor<ModelType> constructor : constructors) {
            // first try to find the constructor with most parameters and @Inject annotation
            if (constructor.hasInjectAnnotation()) {
                return constructor;
            }
            if (constructor.isCanonicalRecordConstructor()) {
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

    private <ModelType> Result<ModelType> newInstanceWithConstructorInjection(
            final ModelClassConstructor<ModelType> constructor,
            final Object adaptable,
            final ModelClass<ModelType> modelClass,
            final DisposalCallbackRegistry registry,
            final @NotNull Map<ValuePreparer, Object> preparedValues)
            throws InstantiationException, InvocationTargetException, IllegalAccessException {
        ConstructorParameter[] parameters = constructor.getConstructorParameters();

        List<Object> paramValues = new ArrayList<>(Arrays.asList(new Object[parameters.length]));
        InjectCallback callback = new SetConstructorParameterCallback(paramValues);

        final BundleContext modelContext = getModelBundleContext(modelClass);
        List<MissingElementException> missingElements = null;
        for (int i = 0; i < parameters.length; i++) {
            RuntimeException t =
                    injectElement(parameters[i], adaptable, registry, callback, preparedValues, modelContext);
            if (t != null) {
                if (missingElements == null) {
                    missingElements = new ArrayList<>();
                }
                missingElements.add(new MissingElementException(parameters[i].getAnnotatedElement(), t));
            }
        }
        if (missingElements != null) {
            MissingElementsException missingElementsException = new MissingElementsException(
                    "Required constructor parameters were not able to be injected on model " + modelClass.getType());
            for (MissingElementException me : missingElements) {
                missingElementsException.addMissingElementExceptions(me);
            }
            return new Result<>(missingElementsException);
        }
        return new Result<>(constructor.newInstance(paramValues.toArray(new Object[paramValues.size()])));
    }

    private Result<Boolean> injectDefaultValue(
            InjectableElement point, InjectAnnotationProcessor processor, InjectCallback callback) {
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
        } else {
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
                if (Arrays.equals(method.getParameterTypes(), newMethod.getParameterTypes())) {
                    return false;
                }
            }
        }
        methods.add(newMethod);
        return true;
    }

    @SuppressWarnings("null")
    private <ModelType> ModelType invokePostConstruct(ModelType object)
            throws InvocationTargetException, IllegalAccessException {
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
            method.setAccessible(true);
            Object result = method.invoke(object);
            if (result instanceof Boolean && !((Boolean) result).booleanValue()) {
                log.debug(
                        "PostConstruct method {}.{} returned false. Returning null model.",
                        method.getDeclaringClass().getName(),
                        method.getName());
                return null;
            }
        }
        return object;
    }

    private RuntimeException setField(InjectableField injectableField, Object createdObject, Object value) {
        Result<Object> result =
                adaptIfNecessary(value, injectableField.getFieldType(), injectableField.getFieldGenericType());
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

    private RuntimeException setConstructorParameter(
            ConstructorParameter constructorParameter, List<Object> parameterValues, Object value) {
        if (constructorParameter.getParameterType() instanceof Class<?>) {
            Result<Object> result = adaptIfNecessary(
                    value, (Class<?>) constructorParameter.getParameterType(), constructorParameter.getType());
            if (result.wasSuccessful()) {
                parameterValues.set(constructorParameter.getParameterIndex(), result.getValue());
                return null;
            } else {
                return result.getThrowable();
            }
        } else {
            return new ModelClassException(String.format(
                    "Constructor parameter with index %d is not a class!", constructorParameter.getParameterIndex()));
        }
    }

    @SuppressWarnings("null")
    private Result<Object> adaptIfNecessary(final Object value, final Class<?> type, final Type genericType) {
        final Object adaptedValue;
        if (!isAcceptableType(type, genericType, value)) {
            if (genericType instanceof ParameterizedType) {
                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                if (value instanceof Collection
                        && (type.equals(Collection.class) || type.equals(List.class))
                        && parameterizedType.getActualTypeArguments().length == 1) {

                    List<Object> result = new ArrayList<>();
                    for (Object valueObject : (Collection<?>) value) {
                        Result<Object> singleValueResult =
                                adapt(valueObject, (Class<?>) parameterizedType.getActualTypeArguments()[0], true);
                        if (singleValueResult.wasSuccessful()) {
                            result.add(singleValueResult.getValue());
                        } else {
                            return singleValueResult;
                        }
                    }
                    adaptedValue = result;
                } else {
                    return new Result<>(new ModelClassException(
                            String.format("%s is neither a parameterized Collection or List", type)));
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
                        String.format(
                                "Could not create model from %s: %s%s",
                                value.getClass(), result.getThrowable().getMessage(), messageSuffix),
                        result.getThrowable()));
            }
        } else if (value instanceof Adaptable) {
            adaptedValue = ((Adaptable) value).adaptTo(type);
            if (adaptedValue == null) {
                return new Result<>(new ModelClassException(
                        String.format("Could not adapt from %s to %s%s", value.getClass(), type, messageSuffix)));
            }
        }
        if (adaptedValue != null) {
            return new Result<>(adaptedValue);
        } else {
            return new Result<>(new ModelClassException(String.format(
                    "Could not adapt from %s to %s%s, because this class is not adaptable!",
                    value.getClass(), type, messageSuffix)));
        }
    }

    @SuppressWarnings("null")
    private static boolean isAcceptableType(Class<?> type, Type genericType, Object value) {
        if (type.isInstance(value)) {
            if ((type == Collection.class || type == List.class)
                    && genericType instanceof ParameterizedType
                    && value instanceof Collection) {
                Iterator<?> it = ((Collection<?>) value).iterator();
                if (!it.hasNext()) {
                    // empty collection, so it doesn't really matter
                    return true;
                } else {
                    // this is not an ideal way to get the actual component type, but erasure...
                    Class<?> actualComponentType = it.next().getClass();
                    Class<?> desiredComponentType =
                            (Class<?>) ((ParameterizedType) genericType).getActualTypeArguments()[0];
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
    protected void activate(final BundleContext bundleContext, final ModelAdapterFactoryConfiguration configuration) {
        this.invocationCountThreadLocal = new ThreadLocal<ThreadInvocationCounter>() {
            @Override
            protected ThreadInvocationCounter initialValue() {
                return new ThreadInvocationCounter(configuration.max_recursion_depth());
            }
        };

        this.adapterCache =
                Collections.synchronizedMap(new WeakHashMap<Object, Map<Class<?>, SoftReference<Object>>>());

        this.scriptEngineFactory = new SlingModelsScriptEngineFactory(bundleContext.getBundle());
        this.listener = new ModelPackageBundleListener(
                bundleContext,
                this,
                this.adapterImplementations,
                bindingsValuesProvidersByContext,
                scriptEngineFactory);

        this.configPrinter = new ModelConfigurationPrinter(bundleContext);
    }

    @Deactivate
    protected void deactivate() {
        this.adapterCache = null;
        this.listener.unregisterAll();
        this.adapterImplementations.removeAll();
        this.configPrinter = null;
        DisposalCallbackRegistryImpl.cleanupDisposables();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindStaticInjectAnnotationProcessorFactory(
            final StaticInjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (this) {
            final Map<Comparable<?>, StaticInjectAnnotationProcessorFactory> factoryMap =
                    new TreeMap<>(this.staticInjectAnnotationProcessorFactories);
            factoryMap.put((Comparable<?>) props, factory);
            this.staticInjectAnnotationProcessorFactories = factoryMap;
            this.adapterImplementations.setStaticInjectAnnotationProcessorFactories(
                    staticInjectAnnotationProcessorFactories.values());
        }
    }

    protected void unbindStaticInjectAnnotationProcessorFactory(
            final StaticInjectAnnotationProcessorFactory factory, final Map<String, Object> props) {
        synchronized (this) {
            final Map<Comparable<?>, StaticInjectAnnotationProcessorFactory> factoryMap =
                    new TreeMap<>(this.staticInjectAnnotationProcessorFactories);
            factoryMap.remove((Comparable<?>) props);
            this.staticInjectAnnotationProcessorFactories = factoryMap;
            this.adapterImplementations.setStaticInjectAnnotationProcessorFactories(
                    staticInjectAnnotationProcessorFactories.values());
        }
    }

    @Reference(name = "viaProvider", cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void bindViaProvider(final ViaProvider viaProvider, final Map<String, Object> props) {
        Class<? extends ViaProviderType> type = viaProvider.getType();
        viaProviders.put(type, viaProvider);
    }

    protected void unbindViaProvider(final ViaProvider viaProvider, final Map<String, Object> props) {
        Class<? extends ViaProviderType> type = viaProvider.getType();
        viaProviders.remove(type, viaProvider);
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
    public Object getModelFromResource(@NotNull Resource resource) {
        Class<?> clazz = this.adapterImplementations.getModelClassForResource(resource);
        if (clazz == null) {
            throw new ModelClassException(
                    "Could find model registered for resource type: " + resource.getResourceType());
        }
        return handleBoundModelResult(internalCreateModel(resource, clazz));
    }

    @Override
    public Object getModelFromRequest(@NotNull SlingHttpServletRequest request) {
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
    @SuppressWarnings("null")
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
    public <T> T exportModelForResource(
            Resource resource, String name, Class<T> targetClass, Map<String, String> options)
            throws ExportException, MissingExporterException {
        Class<?> clazz = this.adapterImplementations.getModelClassForResource(resource);
        if (clazz == null) {
            throw new ModelClassException(
                    "Could find model registered for resource type: " + resource.getResourceType());
        }
        Result<?> result = internalCreateModel(resource, clazz);
        return handleAndExportResult(result, name, targetClass, options);
    }

    @Override
    public <T> T exportModelForRequest(
            SlingHttpServletRequest request, String name, Class<T> targetClass, Map<String, String> options)
            throws ExportException, MissingExporterException {
        Class<?> clazz = this.adapterImplementations.getModelClassForRequest(request);
        if (clazz == null) {
            throw new ModelClassException("Could find model registered for request path: " + request.getServletPath());
        }
        Result<?> result = internalCreateModel(request, clazz);
        return handleAndExportResult(result, name, targetClass, options);
    }

    protected <T> T handleAndExportResult(
            Result<?> result, String name, Class<T> targetClass, Map<String, String> options)
            throws ExportException, MissingExporterException {
        if (result.wasSuccessful()) {
            return exportModel(result.getValue(), name, targetClass, options);
        } else {
            throw result.getThrowable();
        }
    }

    @Override
    public <T> T getModelFromWrappedRequest(
            @NotNull SlingHttpServletRequest request, @NotNull Resource resource, @NotNull Class<T> targetClass) {
        return new ResourceOverridingRequestWrapper(
                        request, resource, adapterManager, scriptEngineFactory, bindingsValuesProvidersByContext)
                .adaptTo(targetClass);
    }

    public void printConfiguration(final PrintWriter printWriter) {
        final Collection<StaticInjectAnnotationProcessorFactory> factories;
        synchronized (this) {
            factories = new ArrayList<>(this.staticInjectAnnotationProcessorFactories.values());
        }
        this.configPrinter.printConfiguration(
                printWriter,
                this.adapterImplementations,
                this.injectors,
                this.injectAnnotationProcessorFactories,
                this.injectAnnotationProcessorFactories2,
                factories,
                this.implementationPickers,
                this.viaProviders);
    }
}
