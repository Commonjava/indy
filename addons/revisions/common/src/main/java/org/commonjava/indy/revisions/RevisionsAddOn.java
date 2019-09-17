/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.revisions;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.indy.model.spi.IndyAddOnID;
import org.commonjava.indy.model.spi.UIRoute;
import org.commonjava.indy.model.spi.UISection;
import org.commonjava.indy.spi.IndyAddOn;

@ApplicationScoped
@Default
@Named( "revisions" )
public class RevisionsAddOn
    implements IndyAddOn
{

    private static final String ROUTE_CHANGELOG = "/revisions/changelog/stores";

    private IndyAddOnID id;

    @Override
    public IndyAddOnID getId()
    {
        /* @formatter:off */
        if ( id == null )
        {
            id =
                new IndyAddOnID().withName( "Revisions" )
                                  .withInitJavascriptHref( "js/revisions.js" )
                                  .withRoute( new UIRoute().withRoute( ROUTE_CHANGELOG )
                                                           .withTemplateHref( "ui-addons/revisions/partials/store-changelog.html" ) )
                                  .withSection( new UISection().withName( "Store Changelogs" )
                                                               .withRoute( ROUTE_CHANGELOG ) );
        }
        /* @formatter:on */

        return id;
    }
}
