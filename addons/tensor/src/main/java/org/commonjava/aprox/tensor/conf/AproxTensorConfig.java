package org.commonjava.aprox.tensor.conf;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "tensor" )
@Named( "use-factory-instead" )
@Alternative
public class AproxTensorConfig
{

    private static final String DEFAULT_TENSOR_DISCOVERY_GROUP = "_tensor";

    private static final int DEFAULT_TENSOR_DISCOVERY_TIMEOUT_SECONDS = 30;

    private String discoveryGroup;

    private Integer discoveryTimeoutSeconds;

    public String getDiscoveryGroup()
    {
        return discoveryGroup == null ? DEFAULT_TENSOR_DISCOVERY_GROUP : discoveryGroup;
    }

    public int getDiscoveryTimeoutSeconds()
    {
        return discoveryTimeoutSeconds == null ? DEFAULT_TENSOR_DISCOVERY_TIMEOUT_SECONDS : discoveryTimeoutSeconds;
    }

    @ConfigName( "discoveryGroup" )
    public void setDiscoveryGroup( final String discoveryGroup )
    {
        this.discoveryGroup = discoveryGroup;
    }

    @ConfigName( "discoveryTimeoutSeconds" )
    public void setDiscoveryTimeoutSeconds( final int discoveryTimeoutSeconds )
    {
        this.discoveryTimeoutSeconds = discoveryTimeoutSeconds;
    }

}
