package org.commonjava.aprox.revisions;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.aprox.dto.UIRoute;
import org.commonjava.aprox.dto.UISection;
import org.commonjava.aprox.spi.AproxAddOn;
import org.commonjava.aprox.spi.AproxAddOnID;

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
                                  .withInitJavascriptHref( "revisions/js/revisions.js" )
                                  .withRoute( new UIRoute().withRoute( ROUTE_CHANGELOG )
                                                           .withTemplateHref( "revisions/partials/store-changelog.html" ) )
                                  .withSection( new UISection().withName( "Store Changelogs" )
                                                               .withRoute( ROUTE_CHANGELOG ) );
        }
        /* @formatter:on */

        return id;
    }
}
