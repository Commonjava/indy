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

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.google.gson.JsonParseException;

public final class JsonUtils
{

    private JsonUtils()
    {
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
