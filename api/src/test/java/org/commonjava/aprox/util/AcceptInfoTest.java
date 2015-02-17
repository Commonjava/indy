package org.commonjava.aprox.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;

public class AcceptInfoTest
{

    @Test
    public void singleAcceptHeader()
        throws Exception
    {
        final List<AcceptInfo> accepts = AcceptInfo.parser( "app", "1" )
                                                   .parse( "text/html" );
        assertThat( accepts.size(), equalTo( 1 ) );

        final AcceptInfo accept = accepts.get( 0 );
        assertThat( accept.getBaseAccept(), equalTo( "text/html" ) );
    }

    @Test
    public void multiAcceptHeader()
        throws Exception
    {
        final List<AcceptInfo> infos =
            AcceptInfo.parser( "app", "1" )
                      .parse( "text/html", "application/xhtml+xml", "application/xml;q=0.9", "*/*;q=0.8" );

        assertThat( infos.size(), equalTo( 4 ) );
    }

    @Test
    public void multiAcceptHeader_SingleHeaderString()
        throws Exception
    {
        final List<AcceptInfo> infos =
            AcceptInfo.parser( "app", "1" )
                      .parse( "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8" );

        assertThat( infos.size(), equalTo( 4 ) );
    }

}
