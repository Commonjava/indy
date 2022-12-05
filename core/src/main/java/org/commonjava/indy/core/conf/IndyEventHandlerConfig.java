/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.conf;

import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.propulsor.config.annotation.SectionName;
import org.commonjava.propulsor.config.section.MapSectionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.io.InputStream;

import static org.commonjava.indy.core.conf.IndyEventHandlerConfig.SECTION_NAME;

@ApplicationScoped
@SectionName(SECTION_NAME)
public class IndyEventHandlerConfig
                extends MapSectionListener
                implements IndyConfigInfo
{

    public static final String SECTION_NAME = "event-handler";

    public static final String HANDLER_DEFAULT = "default";

    public static final String HANDLER_KAFKA = "kafka";

    private String fileEventHandler = HANDLER_DEFAULT;

    public String getFileEventHandler()
    {
        return fileEventHandler;
    }

    public void setFileEventHandler( String fileEventHandler )
    {
        this.fileEventHandler = fileEventHandler;
    }

    @Override
    public void parameter(String name, String value)
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.trace( "Got event-handler config parameter: '{}' with value: '{}'", name, value );
        switch ( name )
        {
            case "file.event.handler":
            {
                this.fileEventHandler = value;
                break;
            }
            default: break;
        }

    }

    @Override
    public String getDefaultConfigFileName()
    {
        return "conf.d/event-handler.conf";
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-event-handler.conf" );
    }
}
