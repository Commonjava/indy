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

        if ( refParts.length < 3 )
        {
            throw new JsonParseException( "Cannot parse versioned project reference (GAV) from parts: ["
                + join( parts, ", " ) + "]" );
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

        if ( refParts.length < 2 )
        {
            throw new JsonParseException( "Cannot parse unversioned project reference (GA) from parts: ["
                + join( parts, ", " ) + "]" );
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

        if ( refParts.length < 6 )
        {
            throw new JsonParseException( "Cannot parse project artifact reference from parts: [" + join( parts, ", " )
                + "]" );
        }

        final String type = refParts[3];
        String classifier = refParts[4];
        if ( classifier.trim()
                       .length() < 1 )
        {
            classifier = null;
        }

        final boolean optional = Boolean.valueOf( refParts[5] );

        return new ArtifactRef( refParts[0], refParts[1], refParts[2], type, classifier, optional );
    }

    public static String formatRef( final ProjectRef ref )
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( ref.getGroupId() )
          .append( ':' )
          .append( ref.getArtifactId() );
        if ( ref instanceof ProjectVersionRef )
        {
            sb.append( ':' )
              .append( ( (ProjectVersionRef) ref ).getVersionString() );
        }

        if ( ref instanceof ArtifactRef )
        {
            final ArtifactRef ar = (ArtifactRef) ref;
            sb.append( ':' )
              .append( ar.getType() );

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
