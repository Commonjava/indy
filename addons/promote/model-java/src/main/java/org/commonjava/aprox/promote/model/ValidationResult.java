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

import java.util.HashMap;
import java.util.Map;

/**
 * Created by jdcasey on 9/11/15.
 */
public class ValidationResult
{
    private boolean valid = true;

    private Map<String, String> validatorErrors = new HashMap<>();

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
}
