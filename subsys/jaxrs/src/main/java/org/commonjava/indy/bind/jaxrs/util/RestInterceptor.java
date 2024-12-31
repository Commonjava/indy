/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.Path;
import java.nio.file.Paths;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.commonjava.indy.util.RequestContextHelper.REST_CLASS;
import static org.commonjava.indy.util.RequestContextHelper.REST_CLASS_PATH;
import static org.commonjava.indy.util.RequestContextHelper.REST_ENDPOINT_PATH;
import static org.commonjava.indy.util.RequestContextHelper.REST_METHOD_PATH;
import static org.commonjava.indy.util.RequestContextHelper.getContext;
import static org.commonjava.indy.util.RequestContextHelper.setContext;

@Interceptor
@REST
public class RestInterceptor
{
    /**
     * Interceptor decorating MDC. Log the beginning/end of REST request and time elapsed.
     */
    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        final Logger logger = LoggerFactory.getLogger( context.getTarget().getClass() );

        Class<?> targetClass = context.getTarget().getClass();
        Path classAnno;
        do
        {
            classAnno = targetClass.getAnnotation( Path.class );
            targetClass = targetClass.getSuperclass();
        }
        while( classAnno == null && targetClass != null );

        String endpointPath = null;

        if ( getContext( REST_CLASS ) == null )
        {
            String targetName = context.getMethod().getDeclaringClass().getSimpleName();
            setContext( REST_CLASS, targetName );

            String classPath = "";
            if ( classAnno != null && getContext( REST_CLASS_PATH ) == null )
            {
                classPath = classAnno.value();
                setContext( REST_CLASS_PATH, classPath );
            }

            Path methAnno = context.getMethod().getAnnotation( Path.class );
            if ( methAnno != null && getContext( REST_METHOD_PATH ) == null  )
            {
                String methodPath = methAnno.value();
                setContext( REST_METHOD_PATH, methodPath );

                endpointPath = Paths.get( classPath, methodPath ).toString();
                setContext( REST_ENDPOINT_PATH, endpointPath );
            }
        }

        //logger.trace( "Interceptor decorating MDC." );

        final boolean isEndpoint = isNotBlank( endpointPath );
        final long begin = currentTimeMillis();
        try
        {
            if ( isEndpoint )
            {
                logger.info( "Start REST: {}", endpointPath );
            }
            return context.proceed();
        }
        finally
        {
            if ( isEndpoint )
            {
                logger.info( "End REST: {}, elapsed: {}ms", endpointPath, ( currentTimeMillis() - begin ) );
            }
        }
    }
}
