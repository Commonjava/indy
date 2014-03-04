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

import static org.apache.commons.lang.StringUtils.join;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public final class JsonUtils
{

    private JsonUtils()
    {
    }

    public static final Set<ProjectRelationship<?>> deserializeRelationships( final JsonObject obj, final JsonDeserializationContext ctx )
    {
        final Set<ProjectRelationship<?>> rels = new LinkedHashSet<ProjectRelationship<?>>();
        final JsonElement arrElem = obj.get( SerializationConstants.RELATIONSHIPS );

        if ( arrElem != null )
        {
            for ( final JsonElement relElem : arrElem.getAsJsonArray() )
            {
                rels.add( (ProjectRelationship<?>) ctx.deserialize( relElem, ProjectRelationship.class ) );
            }
        }

        return rels;
    }

    public static final JsonArray serializeRelationships( final Collection<ProjectRelationship<?>> all, final JsonObject obj,
                                                          final JsonSerializationContext ctx )
    {
        JsonArray arr = null;
        if ( all != null && !all.isEmpty() )
        {
            arr = new JsonArray();

            for ( final ProjectRelationship<?> rel : all )
            {
                arr.add( ctx.serialize( rel ) );
            }

            obj.add( SerializationConstants.RELATIONSHIPS, arr );
        }

        return arr;
    }

    public static void serializeCycles( final Set<EProjectCycle> cycles, final JsonObject obj, final JsonSerializationContext ctx )
    {
        if ( cycles == null )
        {
            return;
        }

        final JsonArray arry = new JsonArray();
        for ( final EProjectCycle cycle : cycles )
        {
            arry.add( ctx.serialize( cycle ) );
        }

        obj.add( SerializationConstants.CYCLES, arry );
    }

    public static Set<EProjectCycle> deserializeCycles( final JsonObject obj, final JsonDeserializationContext ctx )
    {
        final JsonElement cyclesElem = obj.get( SerializationConstants.CYCLES );
        if ( cyclesElem == null )
        {
            return null;
        }

        final JsonArray arry = cyclesElem.getAsJsonArray();
        final Set<EProjectCycle> cycles = new HashSet<EProjectCycle>( arry.size() );

        for ( final JsonElement elem : arry )
        {
            final EProjectCycle cycle = ctx.deserialize( elem, EProjectCycle.class );
            if ( cycle != null )
            {
                cycles.add( cycle );
            }
        }

        return cycles;
    }

    public static ProjectVersionRef parseProjectVersionRef( final String... parts )
        throws JsonParseException
    {
        String[] refParts = parts;
        if ( parts.length == 1 )
        {
            refParts = parts[0].split( ":" );
        }

        if ( refParts.length > 3 )
        {
            return parseArtifactRef( refParts );
        }

        if ( refParts.length < 3 )
        {
            throw new JsonParseException( "Cannot parse versioned project reference (GAV) from parts: [" + join( parts, ", " ) + "]" );
        }

        return new ProjectVersionRef( refParts[0], refParts[1], refParts[2] );
    }

    public static ProjectRef parseProjectRef( final String... parts )
    {
        String[] refParts = parts;
        if ( parts.length == 1 )
        {
            refParts = parts[0].split( ":" );
        }

        if ( refParts.length > 2 )
        {
            return parseProjectVersionRef( refParts );
        }
        else if ( refParts.length > 3 )
        {
            return parseArtifactRef( refParts );
        }

        if ( refParts.length < 2 )
        {
            throw new JsonParseException( "Cannot parse unversioned project reference (GA) from parts: [" + join( parts, ", " ) + "]" );
        }

        return new ProjectRef( refParts[0], refParts[1] );
    }

    public static ArtifactRef parseArtifactRef( final String... parts )
        throws JsonParseException
    {
        String[] refParts = parts;
        if ( parts.length == 1 )
        {
            refParts = parts[0].split( ":" );
        }

        if ( refParts.length < 3 )
        {
            throw new JsonParseException( "Cannot parse project artifact reference from parts: [" + join( parts, ", " ) + "]" );
        }

        int vIdx = 2;
        String type = null;
        if ( refParts.length > 3 )
        {
            vIdx = 3;
            type = refParts[2];
        }

        if ( type != null && ( type.trim()
                                   .length() < 1 || "null".equalsIgnoreCase( type.trim() ) ) )
        {
            type = null;
        }

        String classifier = refParts.length > 4 ? refParts[4] : null;
        if ( classifier != null && ( classifier.trim()
                                               .length() < 1 || "null".equalsIgnoreCase( classifier.trim() ) ) )
        {
            classifier = null;
        }

        final boolean optional = refParts.length > 5 ? Boolean.valueOf( refParts[5] ) : false;

        return new ArtifactRef( refParts[0], refParts[1], refParts[vIdx], type, classifier, optional );
    }

    public static String formatRef( final ProjectRef ref )
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( ref.getGroupId() )
          .append( ':' )
          .append( ref.getArtifactId() );

        if ( ref instanceof ArtifactRef )
        {
            final ArtifactRef ar = (ArtifactRef) ref;
            sb.append( ':' )
              .append( ar.getType() );
        }

        if ( ref instanceof ProjectVersionRef )
        {
            sb.append( ':' )
              .append( ( (ProjectVersionRef) ref ).getVersionString() );
        }

        if ( ref instanceof ArtifactRef )
        {
            final ArtifactRef ar = (ArtifactRef) ref;

            sb.append( ':' );
            if ( ar.getClassifier() != null )
            {
                sb.append( ar.getClassifier() );
            }

            sb.append( ':' )
              .append( ar.isOptional() );
        }

        return sb.toString();
    }

}
