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
 * Describes a section of the UI, to allow add-ons to auto-register UI additions. This corresponds to a menu title (name) and a UI route (path) for
 * display in the UI menuing system. The route is used as a key that links this section to a corresponding {@link UIRoute} via the UI logic.
 */
@ApiModel( "Information about one menu item for use in a Javascript-driven UI" )
public class UISection
{

    private String name;

    private String route;

    public UISection()
    {
    }

    public UISection( final String name, final String route )
    {
        this.name = name;
        this.route = route;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public UISection withName( final String name )
    {
        this.name = name;
        return this;
    }

    public String getRoute()
    {
        return route;
    }

    public void setRoute( final String route )
    {
        this.route = route;
    }

    public UISection withRoute( final String route )
    {
        this.route = route;
        return this;
    }

}
