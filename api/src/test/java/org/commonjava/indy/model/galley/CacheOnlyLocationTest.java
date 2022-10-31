package org.commonjava.indy.model.galley;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.util.LocationUtils;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CacheOnlyLocationTest
{

    @Test
    public void testGetAttribute()
    {
        Group group = new Group( "maven", "test" );
        group.setMetadata( "metadata-timeout", "3600" );
        CacheOnlyLocation location = (CacheOnlyLocation) LocationUtils.toLocation( group );
        int value = location.getAttribute( "metadata-timeout", Integer.class, 60 );
        assertEquals( 3600, value );
    }
}
