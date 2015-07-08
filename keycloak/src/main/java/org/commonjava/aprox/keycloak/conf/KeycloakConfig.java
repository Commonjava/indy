/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.keycloak.conf;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.conf.AbstractAproxConfigInfo;
import org.commonjava.aprox.conf.AbstractAproxFeatureConfig;
import org.commonjava.aprox.conf.AproxConfigClassInfo;
import org.commonjava.aprox.conf.AproxConfigFactory;
import org.commonjava.aprox.conf.AproxConfigInfo;
import org.commonjava.maven.galley.util.PathUtils;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

@SectionName( "keycloak" )
@Alternative
@Named
public class KeycloakConfig
{

    private static final boolean DEFAULT_ENABLED = false;

    private static final String DEFAULT_REALM = "aprox";

    private static final String DEFAULT_KEYCLOAK_JSON = "keycloak/keycloak.json";

    private static final String DEFAULT_KEYCLOAK_UI_JSON = "keycloak/keycloak-ui.json";

    private static final String DEFAULT_SECURITY_BINDINGS_JSON = "keycloak/security-bindings.json";

    private static final String KEYCLOAK_REALM = "keycloak.realm";

    private String realm;

    private Boolean enabled;

    private String keycloakJson;

    private String keycloakUiJson;

    private String securityBindingsJson;

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( final Boolean enabled )
    {
        this.enabled = enabled;
    }

    public String getRealm()
    {
        return realm == null ? DEFAULT_REALM : realm;
    }

    @ConfigName( "realm" )
    public void setRealm( final String realm )
    {
        this.realm = realm;
    }

    public String getKeycloakJson()
    {
        if ( keycloakJson == null )
        {
            return getDefaultConfFile( DEFAULT_KEYCLOAK_JSON );
        }

        return keycloakJson;
    }

    @ConfigName( "keycloak.json" )
    public void setKeycloakJson( final String keycloakJson )
    {
        this.keycloakJson = keycloakJson;
    }

    public String getKeycloakUiJson()
    {
        if ( keycloakUiJson == null )
        {
            return getDefaultConfFile( DEFAULT_KEYCLOAK_UI_JSON );
        }

        return keycloakUiJson;
    }

    @ConfigName( "keycloak-ui.json" )
    public void setKeycloakUiJson( final String keycloakUiJson )
    {
        this.keycloakUiJson = keycloakUiJson;
    }

    public String getSecurityBindingsJson()
    {
        if ( securityBindingsJson == null )
        {
            return getDefaultConfFile( DEFAULT_SECURITY_BINDINGS_JSON );
        }

        return securityBindingsJson;
    }

    @ConfigName( "security-bindings.json" )
    public void setSecurityBindingsJson( final String securityConstraintsJson )
    {
        this.securityBindingsJson = securityConstraintsJson;
    }

    /**
     * Set system properties for keycloak to use when filtering keycloak.json...
     */
    public KeycloakConfig setSystemProperties()
    {
        final Properties properties = System.getProperties();
        properties.setProperty( KEYCLOAK_REALM, getRealm() );
        System.setProperties( properties );

        return this;
    }

    private String getDefaultConfFile( final String confFile )
    {
        String confDir = System.getProperty( AproxConfigFactory.CONFIG_DIR_PROP );
        if ( confDir == null )
        {
            confDir = PathUtils.normalize( System.getProperty( AproxConfigFactory.APROX_HOME_PROP ), "etc/aprox" );
        }

        return PathUtils.normalize( confDir, confFile );
    }

    @javax.enterprise.context.ApplicationScoped
    public static class ConfigInfo
        extends AbstractAproxConfigInfo
    {
        public ConfigInfo()
        {
            super( KeycloakConfig.class );
        }

        @Override
        public String getDefaultConfigFileName()
        {
            return new File( AproxConfigInfo.CONF_INCLUDES_DIR, "keycloak.conf" ).getPath();
        }

        @Override
        public InputStream getDefaultConfig()
        {
            return Thread.currentThread()
                         .getContextClassLoader()
                         .getResourceAsStream( "default-keycloak.conf" );
        }
    }

    @javax.enterprise.context.ApplicationScoped
    public static class FeatureConfig
        extends AbstractAproxFeatureConfig<KeycloakConfig, KeycloakConfig>
    {
        @Inject
        private ConfigInfo info;

        public FeatureConfig()
        {
            super( KeycloakConfig.class );
        }

        @Produces
        @Default
        @ApplicationScoped
        public KeycloakConfig getKeycloakConfig()
            throws ConfigurationException
        {
            return getConfig().setSystemProperties();
        }

        @Override
        public AproxConfigClassInfo getInfo()
        {
            return info;
        }
    }

}
