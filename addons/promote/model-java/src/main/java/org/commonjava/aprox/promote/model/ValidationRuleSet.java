/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.promote.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jdcasey on 9/11/15.
 */
public class ValidationRuleSet
{
    private String name;

    private String storeKeyPattern;

    private List<String> ruleNames;

    private Map<String, String> validationParameters;

    public ValidationRuleSet(){}

    public ValidationRuleSet( String name, String storeKeyPattern, List<String> ruleNames, Map<String, String> validationParameters )
    {
        this.name = name;
        this.storeKeyPattern = storeKeyPattern;
        this.ruleNames = ruleNames;
        this.validationParameters = validationParameters;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( o == null || getClass() != o.getClass() )
        {
            return false;
        }

        ValidationRuleSet that = (ValidationRuleSet) o;

        if ( !getName().equals( that.getName() ) )
        {
            return false;
        }
        if ( !getStoreKeyPattern().equals( that.getStoreKeyPattern() ) )
        {
            return false;
        }
        return !( getRuleNames() != null ?
                !getRuleNames().equals( that.getRuleNames() ) :
                that.getRuleNames() != null );

    }

    @Override
    public int hashCode()
    {
        int result = getName().hashCode();
        result = 31 * result + getStoreKeyPattern().hashCode();
        result = 31 * result + ( getRuleNames() != null ? getRuleNames().hashCode() : 0 );
        return result;
    }

    public String getName()
    {

        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getStoreKeyPattern()
    {
        return storeKeyPattern;
    }

    public void setStoreKeyPattern( String storeKeyPattern )
    {
        this.storeKeyPattern = storeKeyPattern;
    }

    public List<String> getRuleNames()
    {
        return ruleNames;
    }

    public void setRuleNames( List<String> ruleNames )
    {
        this.ruleNames = ruleNames;
    }

    public boolean matchesKey( String keyStr )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.info( "Checking whether pattern: '{}' matches store key: {}", storeKeyPattern, keyStr );
        return storeKeyPattern == null || keyStr.matches( storeKeyPattern );
    }

    public Map<String, String> getValidationParameters()
    {
        return validationParameters;
    }

    public void setValidationParameters( Map<String, String> validationParameters )
    {
        this.validationParameters = validationParameters;
    }
}
