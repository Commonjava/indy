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
package org.commonjava.indy.model.spi;

import io.swagger.annotations.ApiModel;

/**
 * Describes a "route" in the UI, mainly useful for add-ons to register UI additions. This specifies a route (path) and template href, which corresponds
 * to an html fragment, potentially with UI-specific logic, that handles content display for that route. Any UI-side controller init/logic is assumed
 * to be embedded in the template.
 */
@ApiModel( "Information about one route (menu item or similar) for use in a Javascript-driven UI" )
public class UIRoute
{

    private String route;

    private String templateHref;

    public UIRoute()
    {
    }

    public UIRoute( final String route, final String templateHref )
    {
        this.route = route;
        this.templateHref = templateHref;
    }

    public String getRoute()
    {
        return route;
    }

    public void setRoute( final String route )
    {
        this.route = route;
    }

    public UIRoute withRoute( final String route )
    {
        this.route = route;
        return this;
    }

    public String getTemplateHref()
    {
        return templateHref;
    }

    public void setTemplateHref( final String templateHref )
    {
        this.templateHref = templateHref;
    }

    public UIRoute withTemplateHref( final String templateHref )
    {
        this.templateHref = templateHref;
        return this;
    }

}
