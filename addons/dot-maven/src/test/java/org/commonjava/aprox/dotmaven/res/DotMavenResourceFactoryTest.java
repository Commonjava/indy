package org.commonjava.aprox.dotmaven.res;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class DotMavenResourceFactoryTest
{

    @Test
    public void matchSettingsPath()
    {
        assertThat( "/settings-g-public.xml".matches( DotMavenResourceFactory.SETTINGS_PATTERN ), equalTo( true ) );
    }

}
