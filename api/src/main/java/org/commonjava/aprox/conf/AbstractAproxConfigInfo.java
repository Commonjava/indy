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

import org.commonjava.web.config.ConfigUtils;

public abstract class AbstractAproxConfigInfo
    implements AproxConfigInfo
{

    private final Class<?> type;

    private final String sectionName;

    AbstractAproxConfigInfo()
    {
        type = Object.class;
        sectionName = null;
    }

    protected AbstractAproxConfigInfo( final Class<?> type )
    {
        this( type, null );
    }

    protected AbstractAproxConfigInfo( final Class<?> type, final String sectionName )
    {
        this.type = type;
        this.sectionName = sectionName;
    }

    /* (non-Javadoc)
     * @see org.commonjava.aprox.conf.AproxConfigInfo#getConfigurationClass()
     */
    @Override
    public Class<?> getConfigurationClass()
    {
        return type;
    }

    /* (non-Javadoc)
     * @see org.commonjava.aprox.conf.AproxConfigInfo#getSectionName()
     */
    @Override
    public String getSectionName()
    {
        return sectionName;
    }

    @Override
    public String toString()
    {
        final String key = sectionName == null ? ConfigUtils.getSectionName( type ) : sectionName;
        return key + " [" + type.getName() + "]";
    }

}
