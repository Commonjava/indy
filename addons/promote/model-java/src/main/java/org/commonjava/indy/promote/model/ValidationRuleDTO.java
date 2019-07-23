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
package org.commonjava.indy.promote.model;

import io.swagger.annotations.ApiModelProperty;

public class ValidationRuleDTO
{

    @ApiModelProperty(value="Script name for this rule, used for reference in rule-set specifications", required=true)
    private String name;

    @ApiModelProperty( value="Content of validation script", required=true )
    private String spec;

    public ValidationRuleDTO()
    {
    }

    public ValidationRuleDTO( final String name, final String spec )
    {
        this.name = name;
        this.spec = spec;
    }

    public String getName()
    {
        return name;
    }

    public void setName( final String name )
    {
        this.name = name;
    }

    public String getSpec()
    {
        return spec;
    }

    public void setSpec( final String spec )
    {
        this.spec = spec;
    }

    @Override
    public String toString()
    {
        return String.format( "RuleDTO [name=%s]", name );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( spec == null ) ? 0 : spec.hashCode() );
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
        final ValidationRuleDTO other = (ValidationRuleDTO) obj;
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
        if ( spec == null )
        {
            if ( other.spec != null )
            {
                return false;
            }
        }
        else if ( !spec.equals( other.spec ) )
        {
            return false;
        }
        return true;
    }

}
