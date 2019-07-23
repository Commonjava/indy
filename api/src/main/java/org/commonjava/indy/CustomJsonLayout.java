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
package org.commonjava.indy;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Created by yma on 2019/3/26.
 */
public class CustomJsonLayout
        extends JsonLayout
{
    public static final String ENVIRONMENT = "environment";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private String environmentMappings;

    private Map<String, String> envars;

    public String getEnvironmentMappings()
    {
        return environmentMappings;
    }

    public void setEnvironmentMappings( final String environmentMappings )
    {
        this.environmentMappings = environmentMappings;

        String[] mappings = environmentMappings == null ? new String[0] : environmentMappings.split( "\\s*,\\s*" );
        envars = new HashMap<>();
        Stream.of(mappings).forEach( kv ->{
            String[] keyAlias = kv.split( "\\s*=\\s*" );
            if ( keyAlias.length > 1 )
            {
                String value = System.getenv( keyAlias[0].trim() );
                if ( StringUtils.isEmpty( value ) )
                {
                    value = "Unknown";
                }

                envars.put( keyAlias[1].trim(), value );
            }
        } );
    }

    @Override
    protected void addCustomDataToJsonMap( Map<String, Object> map, ILoggingEvent iLoggingEvent )
    {
        super.addCustomDataToJsonMap( map, iLoggingEvent );

        map.put( ENVIRONMENT, envars );
    }
}
