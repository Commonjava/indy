package org.commonjava.aprox.depgraph.util;

import javax.inject.Named;

public final class AnnotationUtils
{

    private AnnotationUtils()
    {
    }

    public static String findNamed( final Class<?> cls )
    {
        final Named annotation = cls.getAnnotation( Named.class );
        return annotation == null ? null : annotation.value();
    }

    public static String findNamed( final Object obj )
    {
        return findNamed( obj.getClass() );
    }

}
