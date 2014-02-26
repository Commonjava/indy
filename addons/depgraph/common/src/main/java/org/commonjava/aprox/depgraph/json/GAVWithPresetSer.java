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

import static org.commonjava.aprox.depgraph.json.SerializationConstants.GAV;

import java.lang.reflect.Type;

import org.commonjava.aprox.depgraph.dto.GAVWithPreset;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.web.json.ser.WebSerializationAdapter;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GAVWithPresetSer
    implements WebSerializationAdapter, JsonSerializer<GAVWithPreset>, JsonDeserializer<GAVWithPreset>
{

    public static final String PRESET = "preset";

    public static GAVWithPreset parsePathSegment( final String src )
        throws DepgraphSerializationException
    {
        final String[] parts = src.split( ":" );
        if ( parts.length > 3 )
        {
            return new GAVWithPreset( new ProjectVersionRef( parts[0], parts[1], parts[2] ), parts[3] );
        }
        else if ( parts.length > 2 )
        {
            return new GAVWithPreset( new ProjectVersionRef( parts[0], parts[1], parts[2] ), null );
        }

        throw new DepgraphSerializationException( "Cannot parse GAV-with-preset from source: '" + src
            + "'. Must be of the form: 'groupId:artifactId:version[:preset]'." );
    }

    public static String formatPathSegment( final GAVWithPreset src )
    {
        final ProjectVersionRef gav = src.getGAV();
        final String preset = src.getPreset();

        return String.format( "{}:{}:{}{}", gav.getGroupId(), gav.getArtifactId(), gav.getVersionString(),
                              ( preset == null ? "" : ":" + preset ) );
    }

    @Override
    public GAVWithPreset deserialize( final JsonElement src, final Type type, final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        final JsonObject obj = src.getAsJsonObject();
        if ( !obj.has( GAV ) )
        {
            throw new JsonParseException( "Required GAV is missing from GAVWithPreset JSON source: '" + src + "'" );
        }

        final JsonObject gavObj = obj.get( GAV )
                                     .getAsJsonObject();

        final ProjectVersionRef ref = ctx.deserialize( gavObj, ProjectVersionRef.class );

        String preset = null;
        if ( obj.has( PRESET ) )
        {
            preset = obj.get( PRESET )
                        .getAsString();
        }

        return new GAVWithPreset( ref, preset );
    }

    @Override
    public JsonElement serialize( final GAVWithPreset src, final Type type, final JsonSerializationContext ctx )
    {
        final JsonObject obj = new JsonObject();
        obj.add( GAV, ctx.serialize( src.getGAV() ) );
        obj.addProperty( PRESET, src.getPreset() );

        return obj;
    }

    @Override
    public void register( final GsonBuilder gsonBuilder )
    {
        gsonBuilder.registerTypeAdapter( GAVWithPreset.class, this );
    }

}
