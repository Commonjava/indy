/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
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
            throw new ConfigurationException( "Cannot open configuration file: %s. Reason: %s", e,
                                              CONFIG_PATH, e.getMessage() );
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
