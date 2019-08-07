package org.commonjava.indy.conf;


import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

@ApplicationScoped
@SectionName( SSLStoreValidationConfig.SECTION_NAME )
public class SSLStoreValidationConfig implements IndyConfigInfo {

    public final static String SECTION_NAME = "ssl_validation";

    private Boolean sslValidation;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public SSLStoreValidationConfig() {
    }

    public Boolean getSslValidation() {
        return sslValidation;
    }

    @ConfigName("_internal.store.validation.enabled")
    public void setSslValidation(Boolean sslValidation) {
        logger.warn("=> SSl Validation value set to : " + sslValidation);
        this.sslValidation = sslValidation;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "ssl_validation.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-ssl_validation.conf" );
    }
}
