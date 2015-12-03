package org.commonjava.aprox.implrepo.conf;

import org.junit.Test;

import java.net.MalformedURLException;

import static org.junit.Assert.assertTrue;

/**
 * Created by jdcasey on 11/30/15.
 */
public class ImpliedRepoConfigTest
{

    @Test
    public void testNexusStagingUrlDisable()
            throws MalformedURLException
    {
        ImpliedRepoConfig config = new ImpliedRepoConfig();
        config.addBlacklist( ".+service.local.staging.*" );

        String url = "http://localhost:8081/service/local/staging/deploy";
        boolean blacklisted = config.isBlacklisted( url );

        assertTrue( "URL should have been blacklisted: " + url, blacklisted );
    }
}
