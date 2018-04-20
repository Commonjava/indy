package org.commonjava.indy.httprox;

import org.commonjava.indy.httprox.handler.AbstractProxyRepositoryCreator;
import org.commonjava.indy.httprox.handler.ProxyCreationResult;
import org.commonjava.indy.subsys.http.util.UserPass;
import org.commonjava.indy.util.UrlInfo;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by ruhan on 4/19/18.
 */
public class AbstractProxyRepositoryCreatorTest
{
    @Ignore
    @Test
    public void testGetDistinctName()
    {
        AbstractProxyRepositoryCreator creator = new AbstractProxyRepositoryCreator() {

            @Override
            public ProxyCreationResult create( String trackingID, String name, String baseUrl, UrlInfo urlInfo,
                                               UserPass userPass, Logger logger )
            {
                return null;
            }
        };

        String host = "127-0-0-1";
        String port = "80";
        List<String> names = new ArrayList<>(  );
        String name_0 = "httprox_" + host + "_" + port;
        String name_1 = name_0 + "_1";
        String name_2 = name_0 + "_2";
        String name_3 = name_0 + "_3";
        String name_4 = name_0 + "_4";

        names.add( name_0 );
        String name = creator.getNextName( names );

        assertThat( name, equalTo( name_1 ) );

        names.add( name_2 );
        names.add( name_1 );
        names.add( name_3 );

        name = creator.getNextName( names );
        assertThat( name, equalTo( name_4 ) );
    }
}
