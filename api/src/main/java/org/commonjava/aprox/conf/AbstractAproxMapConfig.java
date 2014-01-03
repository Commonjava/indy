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
package org.commonjava.aprox.conf;

import org.commonjava.web.config.section.MapSectionListener;

public abstract class AbstractAproxMapConfig
    extends MapSectionListener
{

    private String sectionName;

    protected AbstractAproxMapConfig()
    {
    }

    protected AbstractAproxMapConfig( final String sectionName )
    {
        this.sectionName = sectionName;
    }

    public String getSectionName()
    {
        return sectionName;
    }

}
