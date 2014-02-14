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
package org.commonjava.aprox.util;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;

import com.google.gson.reflect.TypeToken;

@ApplicationScoped
public class AProxModelSerializer
{
    private static final TypeToken<Listing<ArtifactStore>> STORE_LISTING_TYPE_TOKEN = new TypeToken<Listing<ArtifactStore>>()
    {
    };

    //    private static final TypeToken<Listing<Group>> GROUP_LISTING_TYPE_TOKEN = new TypeToken<Listing<Group>>()
    //    {
    //    };
    //
    //    private static final TypeToken<Listing<DeployPoint>> DEPLOY_POINT_LISTING_TYPE_TOKEN = new TypeToken<Listing<DeployPoint>>()
    //    {
    //    };

    @Inject
    @AproxData
    private JsonSerializer restSerializer;

    public String storeListingToString( final Listing<? extends ArtifactStore> listing )
    {
        return restSerializer.toString( listing, STORE_LISTING_TYPE_TOKEN.getType() );
    }

    public String toString( final ArtifactStore store )
    {
        return restSerializer.toString( store );
    }

    public JsonSerializer getJsonSerializer()
    {
        return restSerializer;
    }

    //    public String groupListingToString( final Listing<Group> listing )
    //    {
    //        return restSerializer.toString( listing, GROUP_LISTING_TYPE_TOKEN.getType() );
    //    }
    //
    //    public String deployPointListingToString( final Listing<DeployPoint> listing )
    //    {
    //        return restSerializer.toString( listing, DEPLOY_POINT_LISTING_TYPE_TOKEN.getType() );
    //    }
    //
    //    public String toString( final Group group )
    //    {
    //        return restSerializer.toString( group );
    //    }
    //
    //    public String toString( final DeployPoint deploy )
    //    {
    //        return restSerializer.toString( deploy );
    //    }

}
