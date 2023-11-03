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
package org.commonjava.indy.bind.jaxrs.keycloak;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.ConfigName;
import org.commonjava.propulsor.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

@ApplicationScoped
@SectionName( AuthConfig.SECTION_NAME )
public class AuthConfig
        implements IndyConfigInfo
{
    public static final String SECTION_NAME = "auth.local";

    private static final String ENABLED_PROP = "enabled";

    private static final boolean DEFAULT_ENABLED = true;

    private static final String DEFAULT_SECRET = "indy_default";

    private static final Integer DEFAULT_TOKEN_VALIDITY = 12;

    private Boolean enabled;

    private Integer tokenExpirationHours;

    private String secret;

    private String roles;

    public AuthConfig() {}

    public boolean isEnabled()
    {
        return enabled == null ? DEFAULT_ENABLED : enabled;
    }

    @ConfigName( ENABLED_PROP )
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getTokenExpirationHours()
    {
        return tokenExpirationHours != null ? tokenExpirationHours : DEFAULT_TOKEN_VALIDITY;
    }

    @ConfigName( "token.expiration.hours" )
    public void setTokenExpirationHours(Integer tokenExpirationHours)
    {
        this.tokenExpirationHours = tokenExpirationHours;
    }

    public String getRoles()
    {
        return roles;
    }

    @ConfigName( "token.roles" )
    public void setRoles(String roles)
    {
        this.roles = roles;
    }

    public String getSecret()
    {
        return secret != null ? secret : DEFAULT_SECRET;
    }

    @ConfigName( "token.secret" )
    public void setSecret(String secret)
    {
        this.secret = secret;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/auth.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream( "default-auth.conf" );
    }

}