/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Named;

import org.commonjava.indy.model.spi.IndyAddOnID;
import org.commonjava.indy.model.spi.UIRoute;
import org.commonjava.indy.model.spi.UISection;
import org.commonjava.indy.spi.IndyAddOn;

@ApplicationScoped
@Default
@Named
public class AutoProxAddOn
    implements IndyAddOn
{

    private static final String ROUTE_CALC = "/autoprox/calc";

    private static final String ROUTE_CALC_PREFILL = "/autoprox/calc/view/:type/:name";

    private static final String ROUTE_RULES = "/autoprox/rules";

    private static final String ROUTE_RULES_PREFILL = "/autoprox/rules/view/:name";

    private IndyAddOnID autoproxId;

    @Override
    public IndyAddOnID getId()
    {
        if ( autoproxId == null )
        {
            autoproxId =
                new IndyAddOnID().withName( "AutoProx" )
                                  .withInitJavascriptHref( "js/autoprox.js" )
                                  .withRoute( new UIRoute().withRoute( ROUTE_CALC )
                                                           .withTemplateHref( "ui-addons/autoprox/partials/calc.html" ) )
                                  .withRoute( new UIRoute().withRoute( ROUTE_CALC_PREFILL )
                                                           .withTemplateHref( "ui-addons/autoprox/partials/calc.html" ) )
                                  .withRoute( new UIRoute().withRoute( ROUTE_RULES )
                                                           .withTemplateHref( "ui-addons/autoprox/partials/rules.html" ) )
                                  .withRoute( new UIRoute().withRoute( ROUTE_RULES_PREFILL )
                                                           .withTemplateHref( "ui-addons/autoprox/partials/rules.html" ) )
                                  .withSection( new UISection().withName( "AutoProx Calculator" )
                                                               .withRoute( ROUTE_CALC ) )
                                  .withSection( new UISection().withName( "AutoProx Rules" )
                                                               .withRoute( ROUTE_RULES ) );
        }

        return autoproxId;
    }
}
