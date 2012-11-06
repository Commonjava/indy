package org.commonjava.aprox.dotmaven.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class NameUtilsTest
{

    @Test
    public void checkInvalidURI()
    {
        assertThat( NameUtils.isValidResource( "/.DS_Store" ), equalTo( false ) );
    }

    @Test
    public void checkInvalidURILeaf()
    {
        assertThat( NameUtils.isValidResource( "/path/to/.DS_Store" ), equalTo( false ) );
    }

}
