package org.commonjava.aprox.dotmaven.settings;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.http.client.methods.HttpGet;
import org.apache.maven.settings.Mirror;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Reader;
import org.commonjava.aprox.client.core.AproxClientHttp;
import org.commonjava.aprox.client.core.helper.HttpResources;
import org.junit.Test;

/**
 * Test that the settings.xml generated for a remote repository can be retrieved by simple HTTP GET, and that
 * its localRepository directory and mirror URL are correct.
 */
public class SettingsGeneratedForRemoteRepoTest
    extends AbstractSettingsTest
{
    
    @Test
    public void generateSettingsXml()
        throws Exception
    {
        final AproxClientHttp http = getHttp();

        // all mavdav requests are siblings of the default base-url suffix '/api/'
        final String url = getDotMavenUrl( "settings/remote/settings-central.xml" );
        System.out.println( "Requesting: " + url );

        final HttpResources resources = http.getRaw( new HttpGet( url ) );

        InputStream stream = null;
        Settings settings = null;
        try
        {
            stream = resources.getResponseStream();
            settings = new SettingsXpp3Reader().read( stream );
        }
        finally
        {
            closeQuietly( stream );
            closeQuietly( resources );
        }

        assertThat( settings.getLocalRepository(), equalTo( "${user.home}/.m2/repo-remote-central" ) );
        assertThat( settings.getMirrors(), notNullValue() );
        assertThat( settings.getMirrors()
                            .size(), equalTo( 1 ) );

        final Mirror mirror = settings.getMirrors()
                                      .get( 0 );

        assertThat( mirror.getUrl(), equalTo( http.toAproxUrl( "remote/central" ) ) );
    }

}
