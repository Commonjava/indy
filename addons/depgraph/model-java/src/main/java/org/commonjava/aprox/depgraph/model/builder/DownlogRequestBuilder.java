package org.commonjava.aprox.depgraph.model.builder;

import org.commonjava.aprox.depgraph.model.DownlogRequest;
import org.commonjava.maven.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.cartographer.request.build.GraphRequestOwner;
import org.commonjava.maven.cartographer.request.build.RepositoryContentRequestBuilder;

/**
 * Created by jdcasey on 8/12/15.
 */
public class DownlogRequestBuilder<T extends DownlogRequestBuilder<T, O, R>, O extends GraphRequestOwner<O, R>, R extends DownlogRequest>
    extends RepositoryContentRequestBuilder<T, O, R>
{

    public static final class StandaloneDownlogBuilder
                    extends DownlogRequestBuilder<StandaloneDownlogBuilder, StandaloneRequestOwner<DownlogRequest>, DownlogRequest>
    {
        public StandaloneDownlogBuilder()
        {
            super( new StandaloneRequestOwner<>() );
        }
    }

    public static StandaloneDownlogBuilder newDownlogRequestBuilder()
    {
        return new StandaloneDownlogBuilder();
    }

    private boolean pathOnly;

    private String linePrefix;

    public DownlogRequestBuilder( final O owner )
    {
        super( owner );
    }

    public T withPathOnly( boolean pathOnly )
    {
        this.pathOnly = pathOnly;
        return self;
    }

    public T withLinePrefix( String linePrefix )
    {
        this.linePrefix = linePrefix;
        return self;
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public R build()
    {
        final DownlogRequest recipe = new DownlogRequest();
        configure( recipe );
        configureMultiGraphs( recipe );
        configureRepoContent( recipe );
        confgureDownlog( recipe );

        return (R) recipe;
    }

    protected void confgureDownlog( DownlogRequest recipe )
    {
        recipe.setPathOnly( pathOnly );
        recipe.setLinePrefix( linePrefix );
    }

}
