/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox.rest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.autoprox.data.AutoProxCatalogManager;
import org.commonjava.indy.autoprox.data.AutoProxRuleException;
import org.commonjava.indy.autoprox.data.RuleMapping;
import org.commonjava.indy.autoprox.rest.dto.CatalogDTO;
import org.commonjava.indy.autoprox.rest.dto.RuleDTO;

@ApplicationScoped
public class AutoProxAdminController
{
    @Inject
    private AutoProxCatalogManager catalogManager;

    public CatalogDTO getCatalog()
    {
        return catalogManager.toDTO();
    }

    public RuleDTO storeRule( final RuleDTO dto, final String user )
        throws AutoProxRuleException
    {
        final RuleMapping mapping =
            catalogManager.storeRule( dto.getName(), dto.getSpec(), new ChangeSummary( user,
                                                                                       "Storing rule via REST api." ) );
        return mapping == null ? null : mapping.toDTO();
    }

    public RuleDTO getRule( final String name )
    {
        final RuleMapping mapping = catalogManager.getRuleNamed( name );
        return mapping == null ? null : mapping.toDTO();
    }

    public RuleDTO deleteRule( final String name, final String user )
        throws AutoProxRuleException
    {
        final RuleMapping mapping =
            catalogManager.removeRuleNamed( name, new ChangeSummary( user, "Deleting rule via REST api." ) );
        return mapping == null ? null : mapping.toDTO();
    }

    public void reparseCatalog()
        throws AutoProxRuleException
    {
        catalogManager.parseRules();
    }
}
