/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.commonjava.indy.conf.EnvironmentConfig;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.CDI;
import java.util.Map;

import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.ENVIRONMENT;

/**
 * Created by yma on 2019/3/26.
 */
public class CustomJsonLayout
        extends JsonLayout
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Override
    protected void addCustomDataToJsonMap( Map<String, Object> map, ILoggingEvent iLoggingEvent )
    {
        IndyObjectMapper objectMapper = new IndyObjectMapper( true );
        super.addCustomDataToJsonMap( map, iLoggingEvent );

        if ( !iLoggingEvent.getMDCPropertyMap().isEmpty() )
        {
            Map<String, String> mdcs = (Map<String, String>) map.get( MDC_ATTR_NAME );
            try
            {
                Map<String, String> envars = CDI.current().select( EnvironmentConfig.class ).get().getEnvars();
                mdcs.put( ENVIRONMENT, objectMapper.writeValueAsString( envars ) );
            }
            catch ( JsonProcessingException e )
            {
                mdcs.put( ENVIRONMENT, "{error: \"Envars could not be processed by Jackson.\"}" );
                logger.error( String.format( "Failed to create environment mdc. Reason: %s", e.getMessage() ), e );
            }
            map.put( MDC_ATTR_NAME, mdcs );
        }
    }
}
