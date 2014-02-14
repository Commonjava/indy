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
package org.commonjava.aprox.filer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.galley.KeyedLocation;
import org.commonjava.aprox.util.PathUtils;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.spi.io.PathGenerator;

@Default
@ApplicationScoped
public class KeyBasedPathGenerator
    implements PathGenerator
{

    @Override
    public String getFilePath( final ConcreteResource resource )
    {
        final KeyedLocation kl = (KeyedLocation) resource.getLocation();
        final StoreKey key = kl.getKey();

        final String name = key.getType()
                               .name() + "-" + key.getName();

        return PathUtils.join( name, resource.getPath() );
    }

}
