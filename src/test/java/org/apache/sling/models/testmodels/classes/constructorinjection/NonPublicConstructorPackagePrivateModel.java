package org.apache.sling.models.testmodels.classes.constructorinjection;

import javax.inject.Inject;
import javax.inject.Named;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.models.annotations.Model;

@Model(adaptables = SlingHttpServletRequest.class)
public class NonPublicConstructorPackagePrivateModel {

    @Inject
    NonPublicConstructorPackagePrivateModel(SlingHttpServletRequest request, @Named("attribute") int attribute) {
    }
  
}