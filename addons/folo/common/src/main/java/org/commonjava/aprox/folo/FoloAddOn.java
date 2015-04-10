package org.commonjava.aprox.folo;

import org.commonjava.aprox.model.spi.AproxAddOnID;
import org.commonjava.aprox.spi.AproxAddOn;

public class FoloAddOn
    implements AproxAddOn
{

    public static final String TRACKING_ID_PATH_PARAM = "id";

    private final AproxAddOnID id = new AproxAddOnID().withName( "Folo" );

    @Override
    public AproxAddOnID getId()
    {
        return id;
    }

}
