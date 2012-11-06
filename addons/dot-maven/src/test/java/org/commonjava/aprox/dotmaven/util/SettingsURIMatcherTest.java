package org.commonjava.aprox.dotmaven.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class SettingsURIMatcherTest
{

    @Test
    public void matchSettingsPath()
    {
        assertThat( new SettingsURIMatcher( "/settings/groups/settings-public.xml" ).matches(), equalTo( true ) );
    }

}
