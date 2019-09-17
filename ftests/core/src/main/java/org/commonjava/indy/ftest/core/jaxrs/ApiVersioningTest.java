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
package org.commonjava.indy.ftest.core.jaxrs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.helper.HttpResources;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.stats.IndyVersioning;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.commonjava.indy.stats.IndyVersioning.HEADER_INDY_API_VERSION;
import static org.commonjava.indy.util.ApplicationStatus.GONE;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

public class ApiVersioningTest
                extends AbstractIndyFunctionalTest
{
    private static final String INFO_BASE = "/test/info";

    private static final String ANOTHER_BASE = "/test/another";

    static {
        System.setProperty( "ENV_DEPRECATED_API_FILE", "deprecated-apis-test.properties" );
    }

    @Test
    public void run() throws Exception
    {
        IndyClientHttp m = client.module( IndyRawHttpModule.class ).getHttp();

        Map<String, String> headers = new HashMap<>();
        headers.put( "Accept", "application/json" );

        ObjectMapper mapper = new ObjectMapper();

        HttpResources ret;

        // Get /info. It returns different info according to Indy-API-Version header.

        // try the OFF version and get 410
        headers.put( HEADER_INDY_API_VERSION, "0.5" );
        ret = m.getRaw( INFO_BASE, headers );
        assertEquals( GONE.code(), ret.getStatusCode() );

        // try the deprecated version
        headers.put( HEADER_INDY_API_VERSION, "0.6" );
        ret = m.getRaw( INFO_BASE, headers );
        try (InputStream in = ret.getResponseEntityContent())
        {
            String retBody = IOUtils.toString( in );
            logger.debug( "Deprecated >>>> " + retBody );
            VersioningTestHandlerDeprecated.TestInfo info =
                            mapper.readValue( retBody, VersioningTestHandlerDeprecated.TestInfo.class );
            assertThat( info, notNullValue() );
        }

        // try the latest version
        headers.put( HEADER_INDY_API_VERSION, "" );
        ret = m.getRaw( INFO_BASE, headers );
        try (InputStream in = ret.getResponseEntityContent())
        {
            String retBody = IOUtils.toString( in );
            logger.debug( "Latest >>>> " + retBody );
            VersioningTestHandler.TestInfo info = mapper.readValue( retBody, VersioningTestHandler.TestInfo.class );
            assertThat( info, notNullValue() );
            assertThat( info.getId(), notNullValue() );
        }


        // Get /another. This api is not changed. It returns same object regardless Indy-API-Version header

        String infoDepr, info;

        // try the v1 info
        headers.put( HEADER_INDY_API_VERSION, "0.9" );
        ret = m.getRaw( ANOTHER_BASE, headers );
        try (InputStream in = ret.getResponseEntityContent())
        {
            infoDepr = IOUtils.toString( in );
            logger.debug( "Deprecated >>>> " + infoDepr );
        }

        // try the latest info
        headers.put( HEADER_INDY_API_VERSION, "" );
        ret = m.getRaw( ANOTHER_BASE, headers );
        try (InputStream in = ret.getResponseEntityContent())
        {
            info = IOUtils.toString( in );
            logger.debug( "Latest >>>> " + info );
        }

        assertThat( info, notNullValue() );
        assertEquals( info, infoDepr );
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        List<IndyClientModule> mods = new ArrayList<>();
        Collection<IndyClientModule> fromParent = super.getAdditionalClientModules();

        if ( fromParent != null )
        {
            mods.addAll( fromParent );
        }

        mods.add( new IndyRawHttpModule() );

        return mods;
    }

}
