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
package org.commonjava.aprox.promote.ftest;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;

import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.promote.client.AproxPromoteClientModule;
import org.junit.Before;

public class AbstractPromotionManagerTest
    extends AbstractAproxFunctionalTest
{

    protected final String first = "/first/path";

    protected final String second = "/second/path";

    protected HostedRepository source;

    protected HostedRepository target;

    @Before
    public void setupRepos()
        throws Exception
    {
        final String changelog = "Setup " + name.getMethodName();
        final AproxPromoteClientModule module = client.module( AproxPromoteClientModule.class );
        System.out.printf( "\n\n\n\nBASE-URL: %s\nPROMOTE-URL: %s\nRESUME-URL: %s\nROLLBACK-URL: %s\n\n\n\n",
                           client.getBaseUrl(), module.promoteUrl(), module.resumeUrl(), module.rollbackUrl() );

        source = new HostedRepository( "source" );
        client.stores()
              .create( source, changelog, HostedRepository.class );

        client.content()
              .store( source.getKey()
                            .getType(), source.getName(), first, new ByteArrayInputStream( "This is a test".getBytes() ) );
        client.content()
              .store( source.getKey()
                            .getType(), source.getName(), second,
                      new ByteArrayInputStream( "This is a test".getBytes() ) );

        target = new HostedRepository( "target" );
        client.stores()
              .create( target, changelog, HostedRepository.class );
    }

    @Override
    protected Collection<AproxClientModule> getAdditionalClientModules()
    {
        return Arrays.<AproxClientModule> asList( new AproxPromoteClientModule() );
    }
}
