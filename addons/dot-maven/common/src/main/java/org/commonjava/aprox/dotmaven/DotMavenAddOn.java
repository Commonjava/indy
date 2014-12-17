package org.commonjava.aprox.dotmaven;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.aprox.spi.AproxAddOn;
import org.commonjava.aprox.spi.model.AproxAddOnID;

@ApplicationScoped
@Default
@Named( "dotMaven" )
public class DotMavenAddOn
    implements AproxAddOn
{

    private AproxAddOnID id;

    @Override
    public AproxAddOnID getId()
    {
        if ( id == null )
        {
            id = new AproxAddOnID().withName( "dotMaven" );
        }

        return id;
    }

}
