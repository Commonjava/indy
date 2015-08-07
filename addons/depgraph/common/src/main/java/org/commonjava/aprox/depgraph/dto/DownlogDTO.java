package org.commonjava.aprox.depgraph.dto;

import org.apache.commons.lang.StringUtils;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.galley.CacheOnlyLocation;
import org.commonjava.aprox.util.UriFormatter;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;

public class DownlogDTO
                implements PlainRenderable
{

    private String linePrefix;

    private Set<String> locations;

    public DownlogDTO()
    {
    }

    public DownlogDTO( final Map<ProjectVersionRef, Map<ArtifactRef, ConcreteResource>> contents,
                       final DownlogRecipe recipe, final String baseUri, final UriFormatter uriFormatter )
                    throws AproxWorkflowException
    {
        locations = new TreeSet<>();
        linePrefix = recipe.getLinePrefix();

        for ( final ProjectVersionRef ref : contents.keySet() )
        {
            final Map<ArtifactRef, ConcreteResource> items = contents.get( ref );
            for ( final ConcreteResource item : items.values() )
            {
                Logger logger = LoggerFactory.getLogger( getClass() );
                logger.debug( "Adding: '{}'", item );
                locations.add( formatDownlogEntry( item, recipe, baseUri, uriFormatter ) );
            }
        }
    }

    @Override
    public String render()
    {
        final StringBuilder sb = new StringBuilder();
        for ( final String location : locations )
        {
            if ( StringUtils.isEmpty( location ) )
            {
                continue;
            }

            if ( StringUtils.isNotEmpty( linePrefix ) )
            {
                sb.append( linePrefix );
            }

            sb.append( location ).append( "\n" );
        }

        return sb.toString();
    }

    private String formatDownlogEntry( final ConcreteResource item, final DownlogRecipe recipe, final String baseUri,
                                       final UriFormatter uriFormatter )
                    throws AproxWorkflowException
    {
        String path;
        if ( recipe.isPathOnly() )
        {
            path = item.getPath();
        }
        else if ( recipe.getLocalUrls() || item.getLocation() instanceof CacheOnlyLocation )
        {
            final StoreKey key = ( (CacheOnlyLocation) item.getLocation() ).getKey();

            path = uriFormatter.formatAbsolutePathTo( baseUri, key.getType().singularEndpointName(), key.getName(),
                                                      item.getPath() );
        }
        else
        {
            try
            {
                path = buildUrl( item.getLocation().getUri(), item.getPath() );
            }
            catch ( MalformedURLException e )
            {
                throw new AproxWorkflowException( "Failed to generate remote URL for: %s in location: %s. Reason: %s",
                                                  e, item.getPath(), item.getLocationUri(), e.getMessage() );
            }
        }

        return path;
    }

    public String getLinePrefix()
    {
        return linePrefix;
    }

    public void setLinePrefix( final String linePrefix )
    {
        this.linePrefix = linePrefix;
    }

    public Set<String> getLocations()
    {
        return locations;
    }

    public void setLocations( final Set<String> locations )
    {
        this.locations = locations;
    }
}
