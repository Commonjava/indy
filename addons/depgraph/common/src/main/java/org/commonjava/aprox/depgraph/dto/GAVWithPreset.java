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
package org.commonjava.aprox.depgraph.dto;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class GAVWithPreset
{

    private final ProjectVersionRef gav;

    private final String preset;

    public GAVWithPreset( final ProjectVersionRef gav, final String preset )
    {
        this.gav = gav;
        this.preset = preset;
    }

    public ProjectVersionRef getGAV()
    {
        return gav;
    }

    public String getPreset()
    {
        return preset;
    }

}
