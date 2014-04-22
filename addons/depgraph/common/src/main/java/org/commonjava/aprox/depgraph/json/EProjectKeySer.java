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
package org.commonjava.aprox.depgraph.json;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;

import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EProjectKeySer
    implements JsonSerializer<EProjectKey>, JsonDeserializer<EProjectKey>
{

    @Override
    public EProjectKey deserialize( final JsonElement src, final Type type, final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        final JsonObject obj = src.getAsJsonObject();
        final JsonElement pv = obj.get( SerializationConstants.PROJECT_VERSION );
        if ( pv == null )
        {
            throw new JsonParseException( "Invalid graph key: Missing ProjectVersionRef element '"
                + SerializationConstants.PROJECT_VERSION + "'" );
        }

        final ProjectVersionRef ref = ctx.deserialize( pv, ProjectVersionRef.class );

        final JsonElement u = obj.get( SerializationConstants.SOURCE_URI );

        URI uri;
        try
        {
            uri = u == null ? RelationshipUtils.UNKNOWN_SOURCE_URI : new URI( u.getAsString() );
        }
        catch ( final URISyntaxException e )
        {
            throw new JsonParseException( "Invalid source-uri: '" + u.getAsString() + "': " + e.getMessage(), e );
        }

        return new EProjectKey( uri, ref );
    }

    @Override
    public JsonElement serialize( final EProjectKey src, final Type type, final JsonSerializationContext ctx )
    {
        final JsonObject obj = new JsonObject();
        obj.add( SerializationConstants.PROJECT_VERSION, ctx.serialize( src.getProject() ) );
        obj.addProperty( SerializationConstants.SOURCE_URI, src.getSource()
                                                               .toString() );

        return obj;
    }

}
