package org.commonjava.aprox.depgraph.discover;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.maven.cartographer.event.RelationshipStorageEvent;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class RepoSourceMetadataListener
{

    private static final String FOUND_IN_METADATA = "found-in-repo";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager aprox;

    @Inject
    private CartoDataManager carto;

    public RepoSourceMetadataListener()
    {
    }

    public RepoSourceMetadataListener( final StoreDataManager aprox, final CartoDataManager carto )
    {
        this.aprox = aprox;
        this.carto = carto;
    }

    public void addRepoMetadata( @Observes final RelationshipStorageEvent event )
    {
        final Set<ProjectRelationship<?>> stored = event.getStored();
        if ( stored == null )
        {
            return;
        }

        final Map<URI, Repository> repos = new HashMap<>();
        final Set<URI> unmatchedSources = new HashSet<>();

        final Set<ProjectVersionRef> seen = new HashSet<>();
        for ( final ProjectRelationship<?> rel : stored )
        {
            final ProjectVersionRef ref = rel.getDeclaring()
                                             .asProjectVersionRef();
            if ( seen.contains( ref ) )
            {
                continue;
            }

            seen.add( ref );

            final StringBuilder sb = new StringBuilder();
            final Set<URI> srcs = rel.getSources();
            if ( srcs == null )
            {
                continue;
            }

            for ( final URI src : srcs )
            {
                if ( unmatchedSources.contains( src ) )
                {
                    continue;
                }

                Repository repo = repos.get( src );
                final String scheme = src.getScheme();

                if ( repo != null || StoreType.get( scheme ) == StoreType.repository )
                {
                    if ( repo == null )
                    {
                        final String sub = src.getSchemeSpecificPart();
                        try
                        {
                            repo = aprox.getRepository( sub );
                        }
                        catch ( final ProxyDataException e )
                        {
                            logger.error( "Failed to retrieve repository with name: '%s' for %s metadata association in dependency graph. Reason: %s",
                                          e, sub, FOUND_IN_METADATA, e.getMessage() );
                        }
                    }

                    if ( repo != null )
                    {
                        repos.put( src, repo );

                        if ( sb.length() > 0 )
                        {
                            sb.append( ',' );
                        }

                        sb.append( repo.getKey() )
                          .append( '@' )
                          .append( repo.getUrl() );
                    }
                    else
                    {
                        unmatchedSources.add( src );
                    }
                }
                else
                {
                    unmatchedSources.add( src );
                }
            }

            if ( sb.length() > 0 )
            {
                carto.addMetadata( ref, FOUND_IN_METADATA, FOUND_IN_METADATA );
            }
        }
    }

}
