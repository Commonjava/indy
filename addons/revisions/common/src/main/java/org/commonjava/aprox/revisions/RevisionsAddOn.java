package org.commonjava.aprox.revisions;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.aprox.model.spi.AproxAddOnID;
import org.commonjava.aprox.model.spi.UIRoute;
import org.commonjava.aprox.model.spi.UISection;
import org.commonjava.aprox.spi.AproxAddOn;

@ApplicationScoped
@Default
@Named( "revisions" )
public class RevisionsAddOn
    implements AproxAddOn
{

    private static final String ROUTE_CHANGELOG = "/revisions/changelog/stores";

    private AproxAddOnID id;

    @Override
    public AproxAddOnID getId()
    {
        /* @formatter:off */
        if ( id == null )
        {
            id =
                new AproxAddOnID().withName( "Revisions" )
                                  .withInitJavascriptHref( "ui-addons/revisions/js/revisions.js" )
                                  .withRoute( new UIRoute().withRoute( ROUTE_CHANGELOG )
                                                           .withTemplateHref( "ui-addons/revisions/partials/store-changelog.html" ) )
                                  .withSection( new UISection().withName( "Store Changelogs" )
                                                               .withRoute( ROUTE_CHANGELOG ) );
        }
        /* @formatter:on */

        return id;
    }
}
