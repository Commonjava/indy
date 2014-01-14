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
package org.commonjava.aprox.bind.vertx.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.StoreType;
import org.junit.Test;

public class VertXUriFormatterTest
{

    @Test
    public void absoluteURIToStorePath()
    {
        final String path = "org/commonjava/aprox/aprox-api/0.9/aprox-api-0.9.pom";
        final String storeName = "test-repo";
        final StoreType type = StoreType.repository;

        final String uri = new VertXUriFormatter().formatAbsolutePathTo( type.singularEndpointName(), storeName, path );
        assertThat( uri, equalTo( "/api/1.0/" + type.singularEndpointName() + "/" + storeName + "/" + path ) );
    }

}
