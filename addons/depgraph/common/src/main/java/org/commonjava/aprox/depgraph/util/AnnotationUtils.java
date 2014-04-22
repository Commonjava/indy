/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
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
