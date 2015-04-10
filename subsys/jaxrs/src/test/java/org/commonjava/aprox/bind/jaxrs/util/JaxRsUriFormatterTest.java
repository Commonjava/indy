package org.commonjava.aprox.bind.jaxrs.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class JaxRsUriFormatterTest
{

    @Test
    public void formatAbsoluteUrl()
    {
        final String base = "http://localhost:12345";
        final String path = "/api/path/to/something";
        final String url = new JaxRsUriFormatter().formatAbsolutePathTo( base, path );

        assertThat( url, equalTo( base + path ) );
    }

    @Test
    public void formatAbsolutePath()
    {
        final String base = "/some";
        final String path = "/api/path/to/something";
        final String url = new JaxRsUriFormatter().formatAbsolutePathTo( base, path );

        assertThat( url, equalTo( base + path ) );
    }

    @Test
    public void formatAbsolutePath_BaseNotAbsolute()
    {
        final String base = "some";
        final String path = "/api/path/to/something";
        final String url = new JaxRsUriFormatter().formatAbsolutePathTo( base, path );

        assertThat( url, equalTo( "/" + base + path ) );
    }

}
