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
package org.commonjava.indy.metrics.zabbix.api;

import org.commonjava.test.http.expect.ExpectationServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by xiabai on 5/12/17.
 */
public class ZabbixAPITest
{
    private final static String PATHPARTS = "/api_jsonrpc.php";

    @Rule
    public ExpectationServer server = new ExpectationServer();

    String url = "";

    @Before
    public void start() throws Exception
    {
        server.expect( "POST", server.formatUrl( PATHPARTS ), new ZabbxiAPIHandler() );
        server.start();
    }

    @After
    public void down() throws Exception
    {
//        server.stop();
    }

    @Test
    public void gethostid() throws IOException
    {
        IndyZabbixApi api = new IndyZabbixApi( server.formatUrl( PATHPARTS ) );
        api.init();
        org.junit.Assert.assertEquals( "123", api.getHost( "test" ) );
        api.destroy();
    }

    @Test
    public void getitemid() throws IOException
    {
        IndyZabbixApi api = new IndyZabbixApi( server.formatUrl( PATHPARTS ) );
        api.init();
        org.junit.Assert.assertEquals( "456", api.getItem( "test","test-item","123" ) );
        api.destroy();
    }
}
