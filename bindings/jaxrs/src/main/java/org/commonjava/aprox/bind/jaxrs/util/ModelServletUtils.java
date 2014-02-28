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
package org.commonjava.aprox.bind.jaxrs.util;

import static org.commonjava.web.json.ser.ServletSerializerUtils.fromRequestBody;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.web.json.ser.JsonSerializer;

@ApplicationScoped
public class ModelServletUtils
{

    @Inject
    @AproxData
    private JsonSerializer restSerializer;

    public ArtifactStore storeFromRequestBody( final StoreType st, final HttpServletRequest request )
    {
        switch ( st )
        {
            case hosted:
            {
                return fromRequestBody( request, restSerializer, HostedRepository.class );
            }
            case group:
            {
                return fromRequestBody( request, restSerializer, Group.class );
            }
            default:
            {
                return fromRequestBody( request, restSerializer, RemoteRepository.class );
            }
        }
    }

}
