package org.commonjava.aprox.indexer.inject;

import java.util.List;

import org.apache.maven.index.context.IndexCreator;

public class IndexCreatorSet
{

    private final List<IndexCreator> creators;

    public IndexCreatorSet( final List<IndexCreator> creators )
    {
        this.creators = creators;
    }

    public List<IndexCreator> getCreators()
    {
        return creators;
    }

}
