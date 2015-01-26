package org.commonjava.aprox.promote.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.client.core.Aprox;
import org.junit.Test;

public class AproxPromoteClientModuleUrlsTest
{

    private static final String BASE = "http://localhost:8080/api";

    @Test
    public void promoteUrl()
        throws Exception
    {
        final String url = new Aprox( BASE, new AproxPromoteClientModule() ).module( AproxPromoteClientModule.class )
                                                                            .promoteUrl();

        assertThat( url, equalTo( BASE + "/" + AproxPromoteClientModule.PROMOTE_PATH ) );
    }

    @Test
    public void resumeUrl()
        throws Exception
    {
        final String url = new Aprox( BASE, new AproxPromoteClientModule() ).module( AproxPromoteClientModule.class )
                                                                            .resumeUrl();

        assertThat( url, equalTo( BASE + "/" + AproxPromoteClientModule.RESUME_PATH ) );
    }

    @Test
    public void rollbackUrl()
        throws Exception
    {
        final String url = new Aprox( BASE, new AproxPromoteClientModule() ).module( AproxPromoteClientModule.class )
                                                                            .rollbackUrl();

        assertThat( url, equalTo( BASE + "/" + AproxPromoteClientModule.ROLLBACK_PATH ) );
    }

}
