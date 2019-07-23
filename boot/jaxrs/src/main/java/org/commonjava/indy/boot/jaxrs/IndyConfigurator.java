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
package org.commonjava.indy.boot.jaxrs;

import org.commonjava.indy.conf.IndyConfigFactory;
import org.commonjava.propulsor.boot.BootOptions;
import org.commonjava.propulsor.config.Configurator;
import org.commonjava.propulsor.config.ConfiguratorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class IndyConfigurator
                implements Configurator
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private IndyConfigFactory configFactory;

    @Override
    public void load( BootOptions options ) throws ConfiguratorException
    {
        try
        {
            logger.info( "\n\nLoading Indy configuration factory: {}\n", configFactory );
            configFactory.load( options.getConfig() );
        }
        catch ( final Exception e )
        {
            logger.error( "Failed to configure Indy: {}", e.getMessage(), e );
            throw new ConfiguratorException( "Failed to configure Indy", e );
        }
    }

}
