package org.commonjava.indy.core.change;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.commonjava.indy.core.change.StoreChangeUtil.getDiverged;
import static org.commonjava.indy.core.change.StoreChangeUtil.getDiff;
import static org.junit.Assert.assertTrue;

public class StoreChangeUtilTest
{
    private List<String> newMembers = Arrays.asList( "a", "b", "c", "d" );

    private List<String> oldMembers = Arrays.asList( "a", "b", "e" );

    @Test
    public void getDivergedTest()
    {
        Set<String> affectedMembers = getDiverged( newMembers, oldMembers );
        //System.out.println( affectedMembers );

        List<String> expected = Arrays.asList( "c", "d", "e" );

        assertTrue( affectedMembers.containsAll( expected ) );
        assertTrue( expected.containsAll( affectedMembers ) );
    }

    @Test
    public void getDiffTest()
    {
        Set<String>[] diffMembers = getDiff( newMembers, oldMembers );

        Set<String> added = new HashSet<>( Arrays.asList( "c", "d" ) );
        Set<String> removed = new HashSet<>( Arrays.asList( "e" ) );

        assertTrue( added.containsAll( diffMembers[0] ) );
        assertTrue( diffMembers[0].containsAll( added ) );

        assertTrue( removed.containsAll( diffMembers[1] ) );
        assertTrue( diffMembers[1].containsAll( removed ) );
    }
}
