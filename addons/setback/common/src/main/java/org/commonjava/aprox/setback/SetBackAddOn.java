package org.commonjava.aprox.setback;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.aprox.spi.AproxAddOn;
import org.commonjava.aprox.spi.model.AproxAddOnID;

@ApplicationScoped
@Default
@Named( "setback" )
public class SetBackAddOn
    implements AproxAddOn
{

    private AproxAddOnID id;

    @Override
    public AproxAddOnID getId()
    {
        if ( id == null )
        {
            id = new AproxAddOnID().withName( "AutoProx" );
        }

        return id;
    }
}
