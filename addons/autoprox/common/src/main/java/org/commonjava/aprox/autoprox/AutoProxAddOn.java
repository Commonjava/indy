package org.commonjava.aprox.autoprox;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.aprox.dto.UISection;
import org.commonjava.aprox.spi.AproxAddOn;
import org.commonjava.aprox.spi.AproxAddOnID;

@ApplicationScoped
@Default
@Named( "autoprox" )
public class AutoProxAddOn
    implements AproxAddOn
{

    private AproxAddOnID autoproxId;

    @Override
    public AproxAddOnID getId()
    {
        if ( autoproxId == null )
        {
            autoproxId =
                new AproxAddOnID().withName( "AutoProx" )
                                  .withInitJavascriptHref( "autoprox/js/autoprox.js" )
                                  .withUISection( new UISection().withName( "AutoProx Calculator" )
                                                                 .withRoute( "/autoprox/calc" )
                                                                 .withTemplateHref( "autoprox/partials/calc.html" )
                                                                 .withController( "AutoProxCalculatorCtl" ) );
        }

        return autoproxId;
    }

}
