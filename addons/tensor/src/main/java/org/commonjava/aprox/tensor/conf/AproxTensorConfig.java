package org.commonjava.aprox.tensor.conf;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import org.commonjava.tensor.config.TensorConfig;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "tensor" )
@Named( "use-factory-instead" )
@Alternative
public class AproxTensorConfig
    implements TensorConfig
{

    private static final String DEFAULT_TENSOR_DISCOVERY_GROUP = "_tensor";

    private static final int DEFAULT_TENSOR_DISCOVERY_TIMEOUT_MILLIS = 30000;

    private String discoveryGroup;

    private Long discoveryTimeoutMillis;

    public String getDiscoveryGroup()
    {
        return discoveryGroup == null ? DEFAULT_TENSOR_DISCOVERY_GROUP : discoveryGroup;
    }

    @Override
    public long getDiscoveryTimeoutMillis()
    {
        return discoveryTimeoutMillis == null ? DEFAULT_TENSOR_DISCOVERY_TIMEOUT_MILLIS : discoveryTimeoutMillis;
    }

    @ConfigName( "discoveryGroup" )
    public void setDiscoveryGroup( final String discoveryGroup )
    {
        this.discoveryGroup = discoveryGroup;
    }

    @ConfigName( "discoveryTimeoutMillis" )
    public void setDiscoveryTimeoutMillis( final long discoveryTimeoutMillis )
    {
        this.discoveryTimeoutMillis = discoveryTimeoutMillis;
    }

}
