package org.commonjava.aprox.dotmaven.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class NameUtilsTest
{

    @Test
    public void matchSettingsPath()
    {
        assertThat( NameUtils.isSettingsResource( "/settings-group-public.xml" ), equalTo( true ) );
    }

}
