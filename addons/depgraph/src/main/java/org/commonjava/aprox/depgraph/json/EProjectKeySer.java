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
