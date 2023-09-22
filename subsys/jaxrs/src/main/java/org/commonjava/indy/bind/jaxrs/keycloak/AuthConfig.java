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

    private static final Integer DEFAULT_TOKEN_VALIDITY = 12 * 60 * 60;

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
        return tokenExpirationHours != null ? tokenExpirationHours * 60 * 60 : DEFAULT_TOKEN_VALIDITY;
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