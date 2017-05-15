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
