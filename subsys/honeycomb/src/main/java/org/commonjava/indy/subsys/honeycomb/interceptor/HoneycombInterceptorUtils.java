package org.commonjava.indy.subsys.honeycomb.interceptor;

import io.honeycomb.beeline.tracing.Span;
import org.commonjava.cdi.util.weft.ThreadContext;
import org.commonjava.indy.measure.annotation.MetricWrapperNamed;

import javax.interceptor.InvocationContext;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.LinkedList;

public class HoneycombInterceptorUtils
{

    private static final String SPAN_STACK = "honeycomb-span-stack";

    public static String getMetricNameFromParam( InvocationContext context )
    {
        String name = null;

        Method method = context.getMethod();
        Parameter[] parameters = method.getParameters();
        for ( int i=0; i<parameters.length; i++)
        {
            Parameter param = parameters[i];
            MetricWrapperNamed annotation = param.getAnnotation( MetricWrapperNamed.class );
            if ( annotation != null )
            {
                name = String.valueOf( context.getParameters()[i] );
                break;
            }
        }

        return name;
    }

}
