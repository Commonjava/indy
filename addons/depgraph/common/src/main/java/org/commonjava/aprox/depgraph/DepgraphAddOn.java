package org.commonjava.aprox.depgraph;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.aprox.model.spi.AproxAddOnID;
import org.commonjava.aprox.spi.AproxAddOn;

@ApplicationScoped
@Default
@Named( "depgraph" )
public class DepgraphAddOn
    implements AproxAddOn
{

    private AproxAddOnID id;

    @Override
    public AproxAddOnID getId()
    {
        if ( id == null )
        {
            id = new AproxAddOnID().withName( "Dependency Grapher" );
        }

        return id;
    }

}
