package org.commonjava.aprox.bind.jaxrs;

import io.undertow.servlet.api.DeploymentInfo;

public abstract class AproxDeploymentProvider
{

    public abstract DeploymentInfo getDeploymentInfo();

}
