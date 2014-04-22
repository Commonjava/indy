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
