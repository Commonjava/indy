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
package org.commonjava.indy.conf;

import org.commonjava.propulsor.config.ConfigurationException;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by yma on 2019/3/19.
 */

@ApplicationScoped
@SectionName( EnvironmentConfig.SECTION_NAME )
public class EnvironmentConfig
        extends MapSectionListener
        implements IndyConfigInfo
{
    public static final String SECTION_NAME = "environment";

    public static final String ENV_PREFIX = "mdc.env";

    public static final String HOSTNAME = "HOSTNAME";

    public static final String UNKNOWN = "UNKNOWN";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private Map<String, String> envars = new HashMap<>();

    public EnvironmentConfig()
    {
    }

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "environment.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream( "default-environment.conf" );
    }

    @Override
    public synchronized void parameter( final String name, final String value )
            throws ConfigurationException
    {
        if ( name.startsWith( ENV_PREFIX ) )
        {
            String envKey = name.substring( ENV_PREFIX.length() );
            String enValue = System.getenv( envKey );
            if ( envKey.equals( HOSTNAME ) && enValue == null )
            {
                try
                {
                    enValue = InetAddress.getLocalHost().getHostName();
                }
                catch ( UnknownHostException e )
                {
                    logger.error( String.format( "Unknown host. Reason: %s", e.getMessage() ), e );
                }
            }
            envars.put( value, enValue == null ? UNKNOWN : enValue );
        }
    }

    public Map<String, String> getEnvars()
    {
        return envars;
    }
}
