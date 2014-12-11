package org.commonjava.aprox.autoprox;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.aprox.dto.UIRoute;
import org.commonjava.aprox.dto.UISection;
import org.commonjava.aprox.spi.AproxAddOn;
import org.commonjava.aprox.spi.AproxAddOnID;

@ApplicationScoped
@Default
@Named
public class AutoProxAddOn
    implements AproxAddOn
{

    private static final String ROUTE_CALC = "/autoprox/calc";

    private static final String ROUTE_CALC_PREFILL = "/autoprox/calc/view/:type/:name";

    private static final String ROUTE_RULES = "/autoprox/rules";

    private static final String ROUTE_RULES_PREFILL = "/autoprox/rules/view/:name";

    private AproxAddOnID autoproxId;

    @Override
    public AproxAddOnID getId()
    {
        if ( autoproxId == null )
        {
            autoproxId =
                new AproxAddOnID().withName( "AutoProx" )
                                  .withInitJavascriptHref( "autoprox/js/autoprox.js" )
                                  .withRoute( new UIRoute().withRoute( ROUTE_CALC )
                                                           .withTemplateHref( "autoprox/partials/calc.html" ) )
                                  .withRoute( new UIRoute().withRoute( ROUTE_CALC_PREFILL )
                                                           .withTemplateHref( "autoprox/partials/calc.html" ) )
                                  .withRoute( new UIRoute().withRoute( ROUTE_RULES )
                                                           .withTemplateHref( "autoprox/partials/rules.html" ) )
                                  .withRoute( new UIRoute().withRoute( ROUTE_RULES_PREFILL )
                                                           .withTemplateHref( "autoprox/partials/rules.html" ) )
                                  .withSection( new UISection().withName( "AutoProx Calculator" )
                                                               .withRoute( ROUTE_CALC ) )
                                  .withSection( new UISection().withName( "AutoProx Rules" )
                                                               .withRoute( ROUTE_RULES ) );
        }

        return autoproxId;
    }
}
