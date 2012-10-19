package org.commonjava.aprox.tensor.conf;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.inject.Production;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "tensor" )
@Named( "use-factory-instead" )
@Alternative
public class AproxTensorConfig
{

    @javax.enterprise.context.ApplicationScoped
    public static class AproxTensorFeatureConfig
        extends AbstractAproxFeatureConfig<AproxTensorConfig, AproxTensorConfig>
    {
        @Inject
        private AproxTensorConfigInfo info;

        public AproxTensorFeatureConfig()
        {
            super( AproxTensorConfig.class );
        }

        @Produces
        @Production
        @Default
        public AproxTensorConfig getCacheConfig()
            throws ConfigurationException
        {
            return getConfig();
        }

        @Override
        public AproxConfigInfo getInfo()
        {
            return info;
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static class AproxTensorConfigInfo
        extends AbstractAproxConfigInfo
    {
        public AproxTensorConfigInfo()
        {
            super( AproxTensorConfig.class );
        }
    }

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
