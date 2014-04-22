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
package org.commonjava.aprox.subsys.maven;

public class MavenComponentDefinition<I, T extends I>
{

    private final Class<I> compClass;

    private final Class<T> implClass;

    private String overriddenHint;

    private final String hint;

    public MavenComponentDefinition( final Class<I> compClass, final String overriddenHint, final Class<T> implClass,
                                     final String hint )
    {
        this.compClass = compClass;
        this.implClass = implClass;
        this.overriddenHint = overriddenHint;
        this.hint = hint;
    }

    public MavenComponentDefinition( final Class<I> compClass, final Class<T> implClass, final String hint )
    {
        this.compClass = compClass;
        this.implClass = implClass;
        this.hint = hint;
    }

    public Class<I> getComponentClass()
    {
        return compClass;
    }

    public Class<T> getImplementationClass()
    {
        return implClass;
    }

    public String getHint()
    {
        return hint;
    }

    public String getOverriddenHint()
    {
        return overriddenHint;
    }

}
