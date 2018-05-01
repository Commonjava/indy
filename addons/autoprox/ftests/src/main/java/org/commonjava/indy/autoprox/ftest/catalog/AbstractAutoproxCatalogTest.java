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
package org.commonjava.indy.autoprox.ftest.catalog;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.autoprox.client.AutoProxCatalogModule;
import org.commonjava.indy.autoprox.rest.dto.RuleDTO;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.junit.Assert;

public abstract class AbstractAutoproxCatalogTest
    extends AbstractIndyFunctionalTest
{

    protected AutoProxCatalogModule module;

    protected RuleDTO getRule( final String named, final String ruleScriptResource )
        throws IOException
    {
        final URL resource = Thread.currentThread()
                                   .getContextClassLoader()
                                   .getResource( ruleScriptResource );
        if ( resource == null )
        {
            Assert.fail( "Cannot find classpath resource: " + ruleScriptResource );
        }

        final String spec = IOUtils.toString( resource );

        return new RuleDTO( named, spec );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        module = new AutoProxCatalogModule();
        return Collections.singletonList( module );
    }

}
