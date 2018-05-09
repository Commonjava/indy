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
package org.commonjava.indy.autoprox.rest.dto;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class CatalogDTO
{

    private boolean enabled;

    private List<RuleDTO> rules;

    public CatalogDTO()
    {
    }

    public CatalogDTO( final boolean enabled, final List<RuleDTO> rules )
    {
        this.enabled = enabled;
        this.rules = rules;
    }

    public List<RuleDTO> getRules()
    {
        return rules == null ? Collections.<RuleDTO> emptyList() : rules;
    }

    public void setRules( final List<RuleDTO> rules )
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

    @Override
    public String toString()
    {
        return String.format( "CatalogDTO [enabled=%s]:\n  ", enabled, StringUtils.join( rules, "\n  " ) );
    }

}
