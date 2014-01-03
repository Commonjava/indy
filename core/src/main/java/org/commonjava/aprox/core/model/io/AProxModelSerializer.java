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
package org.commonjava.aprox.core.model.io;

import static org.commonjava.web.json.ser.ServletSerializerUtils.fromRequestBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;

import com.google.gson.reflect.TypeToken;

@javax.enterprise.context.ApplicationScoped
public class AProxModelSerializer
{
    private static final TypeToken<Listing<Repository>> REPO_LISTING_TYPE_TOKEN = new TypeToken<Listing<Repository>>()
    {
    };

    private static final TypeToken<Listing<Group>> GROUP_LISTING_TYPE_TOKEN = new TypeToken<Listing<Group>>()
    {
    };

    private static final TypeToken<Listing<DeployPoint>> DEPLOY_POINT_LISTING_TYPE_TOKEN =
        new TypeToken<Listing<DeployPoint>>()
        {
        };

    @Inject
    @AproxData
    private JsonSerializer restSerializer;

    public Repository repositoryFromRequestBody( final HttpServletRequest request )
    {
        return fromRequestBody( request, restSerializer, Repository.class );
    }

    public Group groupFromRequestBody( final HttpServletRequest request )
    {
        return fromRequestBody( request, restSerializer, Group.class );
    }

    public DeployPoint deployPointFromRequestBody( final HttpServletRequest request )
    {
        return fromRequestBody( request, restSerializer, DeployPoint.class );
    }

    public String repoListingToString( final Listing<Repository> listing )
    {
        return restSerializer.toString( listing, REPO_LISTING_TYPE_TOKEN.getType() );
    }

    public String toString( final Repository repo )
    {
        return restSerializer.toString( repo );
    }

    public String groupListingToString( final Listing<Group> listing )
    {
        return restSerializer.toString( listing, GROUP_LISTING_TYPE_TOKEN.getType() );
    }

    public String deployPointListingToString( final Listing<DeployPoint> listing )
    {
        return restSerializer.toString( listing, DEPLOY_POINT_LISTING_TYPE_TOKEN.getType() );
    }

    public String toString( final Group group )
    {
        return restSerializer.toString( group );
    }

    public String toString( final DeployPoint deploy )
    {
        return restSerializer.toString( deploy );
    }

}
