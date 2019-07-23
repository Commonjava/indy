/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.subsys.http.conf;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.commonjava.util.jhttpc.model.SiteConfigBuilder;
import org.commonjava.util.jhttpc.model.SiteTrustType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.commonjava.util.jhttpc.auth.AttributePasswordManager.PASSWORD_PREFIX;

@ApplicationScoped
@SectionName( IndyHttpConfig.SECTION_NAME )
public class IndyHttpConfig
        extends MapSectionListener
                implements IndyConfigInfo
{
    final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String SECTION_NAME = "http";

    public static final String ENABLED = "enabled";

    public static final String URI = "uri";

    public static final String USER = "user";

    public static final String PROXY_HOST = "proxy.host";

    public static final String PROXY_PORT = "proxy.port";

    public static final String PROXY_USER = "proxy.user";

    public static final String TRUST_TYPE = "trust.type";

    public static final String KEY_CERT_PEM = "key.cert.pem";

    public static final String KEY_CERT_PEM_PATH = "key.cert.pem.path";

    public static final String SERVER_CERT_PEM = "server.cert.pem";

    public static final String SERVER_CERT_PEM_PATH = "server.cert.pem.path";

    public static final String REQUEST_TIMEOUT_SECONDS = "request.timeout.seconds";

    public static final String MAX_CONNECTIONS = "max.connections";

    public static final String KEY_PASSWORD = "key.password";

    public static final String PASSWORD = "password";

    public static final String PROXY_PASSWORD = "proxy.password";

    public IndyHttpConfig()
    {
    }

    public static final String DEFAULT_SITE = "default";

    @Override
    public void sectionComplete( String name ) throws ConfigurationException
    {
        Map<String, SiteConfigBuilder> builderMap = new HashMap<>();
        Map<String, Map<String, Object>> attributesMap = new HashMap<>(); // key: siteId, value: attributes (map)

        Map<String, String> parametersMap = getConfiguration();
        for ( Map.Entry<String, String> et : parametersMap.entrySet() )
        {
            String key = et.getKey();
            String value = et.getValue();
            switch ( key )
            {
                case URI:
                case USER:
                case PROXY_HOST:
                case PROXY_PORT:
                case PROXY_USER:
                case TRUST_TYPE:
                case KEY_CERT_PEM:
                case KEY_CERT_PEM_PATH:
                case SERVER_CERT_PEM:
                case SERVER_CERT_PEM_PATH:
                case REQUEST_TIMEOUT_SECONDS:
                case MAX_CONNECTIONS:
                    withEntry( getSiteConfigBuilder( builderMap, DEFAULT_SITE ), key, value );
                    break;
                case KEY_PASSWORD:
                case PASSWORD:
                case PROXY_PASSWORD:
                    withAttribute( getAttributes( attributesMap, DEFAULT_SITE ), getAttributeName( key ), value );
                    break;
                default:
                    // Not match? Never mind. These are non-default config entries, e.g., keycloak.key.cert.pem=xxx
                    int idx = key.indexOf( "." );
                    String siteId = key.substring( 0, idx );
                    String realKey = key.substring( idx + 1 );
                    if ( isAttribute( realKey ) )
                    {
                        withAttribute( getAttributes( attributesMap, siteId ), getAttributeName( realKey ), value );
                    }
                    else
                    {
                        withEntry( getSiteConfigBuilder( builderMap, siteId ), realKey, value );
                    }
                    break;
            }
        }

        for ( Map.Entry<String, Map<String, Object>> et : attributesMap.entrySet() )
        {
            SiteConfigBuilder builder = builderMap.get( et.getKey() );
            if ( builder == null )
            {
                throw new ConfigurationException( "[http.conf] No site " + et.getKey() + " defined for attributes" );
            }
            builder.withAttributes( et.getValue() );
        }

        for ( Map.Entry<String, SiteConfigBuilder> et : builderMap.entrySet() )
        {
            siteConfigMap.put( et.getKey(), et.getValue().build() );
        }

        logger.debug( "Section complete, name={}, siteConfigMap={}", name, siteConfigMap );
    }

    private String getAttributeName( String key )
    {
        switch ( key )
        {
            case KEY_PASSWORD:
                return PASSWORD_PREFIX + PasswordType.KEY.name();
            case PASSWORD:
                return PASSWORD_PREFIX + PasswordType.USER.name();
            case PROXY_PASSWORD:
                return PASSWORD_PREFIX + PasswordType.PROXY.name();
        }
        return null;
    }

    private boolean isAttribute( String key )
    {
        switch ( key )
        {
            case KEY_PASSWORD:
            case PASSWORD:
            case PROXY_PASSWORD:
                return true;
        }
        return false;
    }

    private void withAttribute( Map<String, Object> attributes, String key, Object value )
    {
        attributes.put( key, value );
    }

    private Map<String, Object> getAttributes( Map<String, Map<String, Object>> attributesMap, String siteId )
    {
        Map<String, Object> attributes = attributesMap.get( siteId );
        if ( attributes == null )
        {
            attributes = new HashMap<>();
            attributesMap.put( siteId, attributes );
        }
        return attributes;
    }

    private Map<String, SiteConfig> siteConfigMap = new HashMap<>();

    public SiteConfig getSiteConfig( String siteId )
    {
        return siteConfigMap.get( siteId );
    }

    private void withEntry( SiteConfigBuilder siteConfigBuilder, String realKey, String value ) throws ConfigurationException
    {
        switch ( realKey )
        {
            case ENABLED:
                break;
            case URI:
                siteConfigBuilder.withUri( value );
                break;
            case USER:
                siteConfigBuilder.withUser( value );
                break;
            case PROXY_HOST:
                siteConfigBuilder.withProxyHost( value );
                break;
            case PROXY_PORT:
                siteConfigBuilder.withProxyPort( Integer.parseInt( value ) );
                break;
            case PROXY_USER:
                siteConfigBuilder.withProxyUser( value );
                break;
            case TRUST_TYPE:
                siteConfigBuilder.withTrustType( SiteTrustType.getType( value ) );
                break;
            case KEY_CERT_PEM:
                siteConfigBuilder.withKeyCertPem( value );
                break;
            case KEY_CERT_PEM_PATH:
                siteConfigBuilder.withKeyCertPem( getPemContent( value ) );
                break;
            case SERVER_CERT_PEM:
                siteConfigBuilder.withServerCertPem( value );
                break;
            case SERVER_CERT_PEM_PATH:
                siteConfigBuilder.withServerCertPem( getPemContent( value ) );
                break;
            case REQUEST_TIMEOUT_SECONDS:
                siteConfigBuilder.withRequestTimeoutSeconds( Integer.parseInt( value ) );
                break;
            case MAX_CONNECTIONS:
                siteConfigBuilder.withMaxConnections( Integer.parseInt( value ) );
                break;
            default:
                throw new ConfigurationException( "[http.conf] Invalid key " + realKey );
        }
    }

    private String getPemContent( String file ) throws ConfigurationException
    {
        try (InputStream stream = new FileInputStream( file ))
        {
            if ( stream == null )
            {
                throw new ConfigurationException("[http.conf] Pem file " + file + " not found");
            }
            return IOUtils.toString( stream );
        }
        catch ( final IOException e )
        {
            throw new ConfigurationException("[http.conf] Failed to read pem file " + file, e);
        }
    }

    private SiteConfigBuilder getSiteConfigBuilder( Map<String, SiteConfigBuilder> siteConfigBuilderMap, String siteId )
    {
        SiteConfigBuilder builder = siteConfigBuilderMap.get( siteId );
        if ( builder == null )
        {
            builder = new SiteConfigBuilder( siteId, null );
            siteConfigBuilderMap.put( siteId, builder );
        }
        return builder;
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/http.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-http.conf" );
    }

}
