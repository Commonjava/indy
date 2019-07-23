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
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;

@ApiModel( description = "Description of an add-on" )
public final class IndyAddOnID
    implements Comparable<IndyAddOnID>
{

    private String name;

    private String description;

    @ApiModelProperty( "List of router entries (mainly for use in the UI)" )
    private List<UIRoute> routes;

    @ApiModelProperty( "List of menu sections (mainly for use in the UI)" )
    private List<UISection> sections;

    @ApiModelProperty( "Path to the Javascript needed to drive the UI for this add-on" )
    private String initJavascriptHref;

    public String getInitJavascriptHref()
    {
        return initJavascriptHref;
    }

    public void setInitJavascriptHref( final String initJavascriptHref )
    {
        this.initJavascriptHref = initJavascriptHref;
    }

    public IndyAddOnID withInitJavascriptHref( final String initJavascriptHref )
    {
        this.initJavascriptHref = initJavascriptHref;
        return this;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public IndyAddOnID withName( final String name )
    {
        this.name = name;
        return this;
    }

    public List<UISection> getSections()
    {
        return sections;
    }

    public void setSections( final List<UISection> sections )
    {
        this.sections = sections;
    }

    public IndyAddOnID withSection( final UISection section )
    {
        if ( sections == null )
        {
            sections = new ArrayList<UISection>();
        }

        sections.add( section );

        return this;
    }

    public List<UIRoute> getRoutes()
    {
        return routes;
    }

    public void setRoutes( final List<UIRoute> routes )
    {
        this.routes = routes;
    }

    public IndyAddOnID withRoute( final UIRoute route )
    {
        if ( routes == null )
        {
            routes = new ArrayList<UIRoute>();
        }

        routes.add( route );

        return this;
    }

    @Override
    public String toString()
    {
        return "IndyAddOnID [" + name + "]";
    }

    @Override
    public int compareTo( final IndyAddOnID other )
    {
        return name.compareTo( other.name );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final IndyAddOnID other = (IndyAddOnID) obj;
        if ( name == null )
        {
            if ( other.name != null )
            {
                return false;
            }
        }
        else if ( !name.equals( other.name ) )
        {
            return false;
        }
        return true;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( final String description )
    {
        this.description = description;
    }
}
