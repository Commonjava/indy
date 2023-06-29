/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.service.config;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@SectionName( "repo-service" )
@ApplicationScoped
public class RepositoryServiceConfig
        implements IndyConfigInfo
{
    private Boolean enabled = Boolean.FALSE;

    private String serviceUrl;

    private Integer requestTimeout = 60;

    private Boolean authEnabled = Boolean.FALSE;

    private String keycloakAuthUrl;

    private String keycloakAuthRealm;

    private String keycloakClientId;

    private String keycloakClientSecret;

    public Boolean isEnabled()
    {
        return enabled;
    }

    @ConfigName( "enabled" )
    public void setEnabled( Boolean enabled )
    {
        this.enabled = enabled;
    }

    public String getServiceUrl()
    {
        return serviceUrl;
    }

    @ConfigName( "service.url" )
    public void setServiceUrl( String serviceUrl )
    {
        this.serviceUrl = serviceUrl;
    }

    public Integer getRequestTimeout()
    {
        return requestTimeout;
    }

    @ConfigName( "service.request.timeout" )
    public void setRequestTimeout( Integer requestTimeout )
    {
        this.requestTimeout = requestTimeout;
    }

    public Boolean isAuthEnabled()
    {
        return authEnabled;
    }

    @ConfigName( "auth.enabled" )
    public void setAuthEnabled( Boolean authEnabled )
    {
        this.authEnabled = authEnabled;
    }

    public String getKeycloakAuthUrl()
    {
        return keycloakAuthUrl;
    }

    @ConfigName( "keycloak.auth.url" )
    public void setKeycloakAuthUrl( String keycloakAuthUrl )
    {
        this.keycloakAuthUrl = keycloakAuthUrl;
    }

    public String getKeycloakAuthRealm()
    {
        return keycloakAuthRealm;
    }

    @ConfigName( "keycloak.auth.realm" )
    public void setKeycloakAuthRealm( String keycloakAuthRealm )
    {
        this.keycloakAuthRealm = keycloakAuthRealm;
    }

    public String getKeycloakClientId()
    {
        return keycloakClientId;
    }

    @ConfigName( "keycloak.auth.clientid" )
    public void setKeycloakClientId( String keycloakClientId )
    {
        this.keycloakClientId = keycloakClientId;
    }

    public String getKeycloakClientSecret()
    {
        return keycloakClientSecret;
    }

    @ConfigName( "keycloak.auth.clientsecret" )
    public void setKeycloakClientSecret( String keycloakClientSecret )
    {
        this.keycloakClientSecret = keycloakClientSecret;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "repo-service.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-repo-service.conf" );
    }
}
