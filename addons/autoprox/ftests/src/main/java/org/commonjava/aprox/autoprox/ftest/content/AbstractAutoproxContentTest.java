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
package org.commonjava.aprox.autoprox.ftest.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.autoprox.client.AutoProxCatalogModule;
import org.commonjava.aprox.autoprox.rest.dto.RuleDTO;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.test.fixture.core.CoreServerFixture;
import org.commonjava.aprox.test.fixture.core.HttpTestFixture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

public abstract class AbstractAutoproxContentTest
    extends AbstractAproxFunctionalTest
{

    public static final String NAME = "test";

    @Rule
    public final HttpTestFixture http = new HttpTestFixture( "remote" );

    @Before
    public void setup()
        throws Exception
    {
        if ( initRule() )
        {
//            installRule( "0001-simple-rule-live.groovy", "rules/simple-rule-live.groovy" );
            expectRepoAutoCreation( NAME );
        }
    }

    private boolean initRule()
    {
        return true;
    }

    @Override
    protected void initTestData( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestData( fixture );

        String spec = readTestResource( "rules/simple-rule-live.groovy" );
        spec = spec.replace( "@baseUri@", http.getBaseUri() );
        writeDataFile( "autoprox/0001-simple-rule-live.groovy", spec );
    }

//    protected RuleDTO installRule( final String named, final String ruleScriptResource )
//        throws IOException, AproxClientException
//    {
//        final URL resource = Thread.currentThread()
//                                   .getContextClassLoader()
//                                   .getResource( ruleScriptResource );
//        if ( resource == null )
//        {
//            Assert.fail( "Cannot find classpath resource: " + ruleScriptResource );
//        }
//
//        String spec = IOUtils.toString( resource );
//        spec = spec.replace( "@baseUri@", http.getBaseUri() );
//
//        return client.module( AutoProxCatalogModule.class )
//                     .storeRule( new RuleDTO( named, spec ) );
//    }

    protected void expectRepoAutoCreation( final String named )
        throws Exception
    {
        http.expect( "HEAD", http.formatUrl( named ), 200 );
    }

    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( new AutoProxCatalogModule() );
    }

}
