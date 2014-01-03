/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.dto;

import org.commonjava.aprox.depgraph.json.GAVWithPresetSer;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.web.json.ser.JsonAdapters;

@JsonAdapters( GAVWithPresetSer.class )
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
