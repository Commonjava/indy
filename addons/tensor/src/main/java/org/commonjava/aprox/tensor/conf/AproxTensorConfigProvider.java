package org.commonjava.aprox.tensor.conf;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.aprox.inject.Production;
import org.commonjava.web.config.ConfigurationException;

@ApplicationScoped
public class AproxTensorConfigProvider
    extends AbstractAproxFeatureConfig<AproxTensorConfig, AproxTensorConfig>
{
    @ApplicationScoped
    public static class AproxTensorConfigInfo
        extends AbstractAproxConfigInfo
    {
        public AproxTensorConfigInfo()
        {
            super( AproxTensorConfig.class );
        }
    }

    @Inject
    private AproxTensorConfigInfo info;

    public AproxTensorConfigProvider()
    {
        super( AproxTensorConfig.class );
    }

    @Produces
    @Production
    @Default
    public AproxTensorConfig getTensorConfig()
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