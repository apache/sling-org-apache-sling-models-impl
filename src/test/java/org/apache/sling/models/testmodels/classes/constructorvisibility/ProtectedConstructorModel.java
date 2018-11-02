package org.apache.sling.models.testmodels.classes.constructorvisibility;

import javax.inject.Inject;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = SlingHttpServletRequest.class)
public class ProtectedConstructorModel {

    @Inject
    protected ProtectedConstructorModel() {
    }

}
