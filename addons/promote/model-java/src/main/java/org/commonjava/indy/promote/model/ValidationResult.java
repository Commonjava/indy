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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 9/11/15.
 */
public class ValidationResult
{
    @ApiModelProperty( value="Whether validation succeeded", required=true )
    private boolean valid = true;

    @ApiModelProperty( "Mapping of rule name to error message for any failing validations" )
    private Map<String, String> validatorErrors = new HashMap<>();

    @ApiModelProperty( "Name of validation rule-set applied" )
    private String ruleSet;

    public void addValidatorError( String validatorName, String message )
    {
        valid = false;
        validatorErrors.put( validatorName, message );
    }

    public boolean isValid()
    {
        return valid;
    }

    public void setValid( boolean valid )
    {
        this.valid = valid;
    }

    public Map<String, String> getValidatorErrors()
    {
        return validatorErrors;
    }

    public void setValidatorErrors( Map<String, String> validatorErrors )
    {
        this.validatorErrors = validatorErrors;
    }

    public void setRuleSet( String ruleSet )
    {
        this.ruleSet = ruleSet;
    }

    public String getRuleSet()
    {
        return ruleSet;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof ValidationResult ) )
        {
            return false;
        }

        ValidationResult that = (ValidationResult) o;

        if ( isValid() != that.isValid() )
        {
            return false;
        }
        if ( getValidatorErrors() != null ?
                !getValidatorErrors().equals( that.getValidatorErrors() ) :
                that.getValidatorErrors() != null )
        {
            return false;
        }
        return getRuleSet() != null ? getRuleSet().equals( that.getRuleSet() ) : that.getRuleSet() == null;

    }

    @Override
    public int hashCode()
    {
        int result = ( isValid() ? 1 : 0 );
        result = 31 * result + ( getValidatorErrors() != null ? getValidatorErrors().hashCode() : 0 );
        result = 31 * result + ( getRuleSet() != null ? getRuleSet().hashCode() : 0 );
        return result;
    }

    @Override
    public String toString()
    {
        return "ValidationResult{" +
                "valid=" + valid +
                ", validatorErrors=" + validatorErrors +
                ", ruleSet='" + ruleSet + '\'' +
                '}';
    }
}
