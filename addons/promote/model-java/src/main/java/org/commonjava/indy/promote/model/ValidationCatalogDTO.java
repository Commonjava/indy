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

import org.apache.commons.lang.StringUtils;

import java.util.Collections;
import java.util.Map;

public class ValidationCatalogDTO
{

    private boolean enabled;

    private Map<String, ValidationRuleDTO> rules;

    private Map<String, ValidationRuleSet> ruleSets;

    public ValidationCatalogDTO()
    {
    }

    public ValidationCatalogDTO( final boolean enabled, final Map<String, ValidationRuleDTO> rules,
                                 final Map<String, ValidationRuleSet> ruleSets )
    {
        this.enabled = enabled;
        this.rules = rules;
        this.ruleSets = ruleSets;
    }

    public Map<String, ValidationRuleDTO> getRules()
    {
        return rules == null ? Collections.<String, ValidationRuleDTO>emptyMap() : rules;
    }

    public void setRules( final Map<String, ValidationRuleDTO> rules )
    {
        this.rules = rules;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled( final boolean enabled )
    {
        this.enabled = enabled;
    }

    public Map<String, ValidationRuleSet> getRuleSets()
    {
        return ruleSets;
    }

    public void setRuleSets( Map<String, ValidationRuleSet> ruleSets )
    {
        this.ruleSets = ruleSets;
    }

    @Override
    public String toString()
    {
        return String.format( "ValidationCatalogDTO [enabled=%s]:\nRules:\n\n  %s\n\nRule-Sets:\n\n  %s\n\n", enabled,
                              rules == null ? "none" : StringUtils.join( rules.keySet(), "\n  " ),
                              ruleSets == null ? "none" : StringUtils.join( ruleSets.keySet(), "\n  " ) );
    }

}
