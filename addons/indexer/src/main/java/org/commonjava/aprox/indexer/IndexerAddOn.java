package org.commonjava.aprox.indexer;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.aprox.model.spi.AproxAddOnID;
import org.commonjava.aprox.spi.AproxAddOn;

@ApplicationScoped
@Default
@Named( "indexer" )
public class IndexerAddOn
    implements AproxAddOn
{

    private AproxAddOnID id;

    @Override
    public AproxAddOnID getId()
    {
        if ( id == null )
        {
            id = new AproxAddOnID().withName( "Indexer" );
        }

        return id;
    }

}
