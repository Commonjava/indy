package org.commonjava.indy.conf;


import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

@ApplicationScoped
@SectionName( InternalFeatureConfig.SECTION_NAME )
public class InternalFeatureConfig implements IndyConfigInfo {

    public final static String SECTION_NAME = "_internal";

    private Boolean storeValidation;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public InternalFeatureConfig() {
    }

    public Boolean getStoreValidation() {
        return storeValidation == null ? Boolean.FALSE : storeValidation;
    }

    @ConfigName("store.validation.enabled")
    public void setStoreValidation( Boolean storeValidation) {
        logger.warn("=> Store Validation value set to : " + storeValidation);
        this.storeValidation = storeValidation;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "internal-features.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-internal-features.conf" );
    }
}
