package org.commonjava.indy.bind.jaxrs.util;

import org.slf4j.LoggerFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.ws.rs.Path;
import java.nio.file.Paths;

import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.REST_CLASS;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.REST_CLASS_PATH;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.REST_ENDPOINT_PATH;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.REST_METHOD_PATH;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.getContext;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.setContext;

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

                String endpointPath = Paths.get( classPath, methodPath ).toString();
                setContext( REST_ENDPOINT_PATH, endpointPath );
            }
        }

        LoggerFactory.getLogger( context.getTarget().getClass() ).info( "Interceptor decorating MDC." );

        return context.proceed();
    }


}
