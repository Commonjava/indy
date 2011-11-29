/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.conf;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.aprox.core.inject.AproxData;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.web.config.ConfigurationException;
import org.commonjava.web.config.DefaultConfigurationListener;
import org.commonjava.web.config.dotconf.DotConfConfigurationReader;

@Singleton
public class ProxyConfigurationFactory
    extends DefaultConfigurationListener
{

    private static final String CONFIG_PATH = "/etc/aprox/main.conf";

    private DefaultProxyConfiguration proxyConfig;

    public ProxyConfigurationFactory()
        throws ConfigurationException
    {
        super( DefaultProxyConfiguration.class );
    }

    @PostConstruct
    protected void load()
        throws ConfigurationException
    {
        InputStream stream = null;
        try
        {
            stream = new FileInputStream( CONFIG_PATH );
            new DotConfConfigurationReader( this ).loadConfiguration( stream );
        }
        catch ( final IOException e )
        {
            throw new ConfigurationException( "Cannot open configuration file: %s. Reason: %s", e, CONFIG_PATH,
                                              e.getMessage() );
        }
        finally
        {
            closeQuietly( stream );
        }
    }

    @Produces
    public ProxyConfiguration getProxyConfiguration()
    {
        return proxyConfig;
    }

    @Produces
    @AproxData
    @Default
    public CouchDBConfiguration getCouchDBConfiguration()
    {
        return proxyConfig.getDatabaseConfig();
    }

    @Override
    public void configurationComplete()
        throws ConfigurationException
    {
        proxyConfig = getConfiguration( DefaultProxyConfiguration.class );
    }

}
