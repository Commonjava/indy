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
package org.commonjava.indy.bind.jaxrs.util;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.resteasy.cdi.CdiInjectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class CdiInjectorFactoryImpl
    extends CdiInjectorFactory
{

    @Override
    protected BeanManager lookupBeanManager()
    {
        final BeanManager bmgr = CDI.current()
                  .getBeanManager();

        Logger logger = LoggerFactory.getLogger( getClass() );

        if ( logger.isDebugEnabled() )
        {
            Set<Bean<?>> mappers = bmgr.getBeans( ObjectMapper.class );
            mappers.forEach( bean -> {
                CreationalContext ctx = bmgr.createCreationalContext( null );
                logger.debug( "Found ObjectMapper: {}", bean.create( ctx ) );
                ctx.release();
            } );

            logger.debug( "\n\n\n\nRESTEasy CDI Injector Factory Using BeanManager: {} (@{})\n\n\n\n", bmgr, bmgr.hashCode() );
        }
        return bmgr;
    }

}
