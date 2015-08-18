/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.depgraph.model.builder;

import org.commonjava.aprox.depgraph.model.DownlogRequest;
import org.commonjava.cartographer.request.build.GraphRequestOwner;
import org.commonjava.cartographer.request.build.RepositoryContentRequestBuilder;

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
