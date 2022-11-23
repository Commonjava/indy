/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.conf;


import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

import static java.lang.Boolean.TRUE;

@ApplicationScoped
@SectionName( InternalFeatureConfig.SECTION_NAME )
public class InternalFeatureConfig implements IndyConfigInfo {

    public final static String SECTION_NAME = "_internal";

    private Boolean storeValidation;

    private Boolean foloISPNQueryPaginationEnabled;

    private boolean mavenMetadataCacheEnabled = TRUE;

    /**
     * Indy disables a remote store when transfer error happens, and try to re-enable it
     * after a timeout specified either by global 'storeDisableTimeoutSeconds' or store specific timeout.
     * Auto disable-and-re-enabling a store may not be very useful. Thus, false by default.
     */
    private boolean storeAutoDisableAndReEnable;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public InternalFeatureConfig() {
    }

    public Boolean getFoloISPNQueryPaginationEnabled()
    {
        return foloISPNQueryPaginationEnabled == null ? TRUE : foloISPNQueryPaginationEnabled;
    }

    @ConfigName("folo.ispn.query.pagination.enabled")
    public void setFoloISPNQueryPaginationEnabled( Boolean foloISPNQueryPaginationEnabled )
    {
        this.foloISPNQueryPaginationEnabled = foloISPNQueryPaginationEnabled;
    }

    public Boolean getStoreValidation() {
        return storeValidation == null ? TRUE : storeValidation;
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

    public boolean isMavenMetadataCacheEnabled() {
        return mavenMetadataCacheEnabled;
    }

    @ConfigName("maven.metadata.cache.enabled")
    public void setMavenMetadataCacheEnabled(boolean mavenMetadataCacheEnabled) {
        this.mavenMetadataCacheEnabled = mavenMetadataCacheEnabled;
    }

    public boolean isStoreAutoDisableAndReEnable() {
        return storeAutoDisableAndReEnable;
    }

    @ConfigName("store.auto.disable.reenable")
    public void setStoreAutoDisableAndReEnable(boolean storeAutoDisableAndReEnable) {
        this.storeAutoDisableAndReEnable = storeAutoDisableAndReEnable;
    }
}
