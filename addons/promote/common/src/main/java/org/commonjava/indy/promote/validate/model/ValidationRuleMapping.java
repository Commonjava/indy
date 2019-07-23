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
package org.commonjava.indy.promote.validate.model;

import org.commonjava.indy.promote.model.ValidationRuleDTO;

public final class ValidationRuleMapping
        implements Comparable<ValidationRuleMapping>
{

    private final ValidationRule rule;

    private final String name;

    private final String spec;

    public ValidationRuleMapping( final String name, final String match, final String spec,
                                  final ValidationRule factory )
    {
        this.name = name;
        this.spec = spec;
        this.rule = factory;
    }

    public ValidationRuleMapping( final String name, final String spec, final ValidationRule factory )
    {
        this.name = name;
        this.spec = spec;
        this.rule = factory;
    }

    public ValidationRuleMapping( final String name, final ValidationRuleMapping ruleMapping )
    {
        this.name = name;
        this.rule = ruleMapping.getRule();
        this.spec = ruleMapping.getSpecification();
    }

    public ValidationRuleDTO toDTO()
    {
        return new ValidationRuleDTO( name, spec );
    }

    public String getName()
    {
        return name;
    }

    public ValidationRule getRule()
    {
        return rule;
    }

    public String getSpecification()
    {
        return spec;
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
        final ValidationRuleMapping other = (ValidationRuleMapping) obj;
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

    @Override
    public int compareTo( final ValidationRuleMapping other )
    {
        return name.compareTo( other.name );
    }

}
