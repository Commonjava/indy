package org.commonjava.indy.bind.jaxrs.util;

import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.Path;

import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.REST_CLASS;
import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.REST_CLASS_PATH;
import static org.commonjava.indy.bind.jaxrs.RequestContextConstants.REST_METHOD_PATH;

@Interceptor
@REST
public class RestInterceptor
{
    @AroundInvoke
    public Object operation( InvocationContext context ) throws Exception
    {
        Class<?> targetClass = context.getTarget().getClass();
        Path classAnno = null;
        do
        {
            classAnno = targetClass.getAnnotation( Path.class );
            targetClass = targetClass.getSuperclass();
        }
        while( classAnno == null && targetClass != null );

        if ( MDC.get( REST_CLASS ) == null )
        {
            String targetName = context.getTarget().getClass().getSimpleName();
            MDC.put( REST_CLASS, targetName );

            if ( classAnno != null && MDC.get( REST_CLASS_PATH ) == null )
            {
                MDC.put( REST_CLASS_PATH, classAnno.value() );
            }

            Path methAnno = context.getMethod().getAnnotation( Path.class );
            if ( methAnno != null && MDC.get( REST_METHOD_PATH ) == null  )
            {
                MDC.put( REST_METHOD_PATH, methAnno.value() );
            }
        }

        LoggerFactory.getLogger( context.getTarget().getClass() ).info( "Interceptor decorating MDC." );

        return context.proceed();
    }


}
