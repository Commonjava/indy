package org.commonjava.indy.promote.validate;

import groovy.lang.Closure;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

public class PromotionValidationToolsTest
{
    @Test
    public void testParalleledInBatch()
    {
        Executor executor = Executors.newCachedThreadPool();
        PromotionValidationTools tools =
                        new PromotionValidationTools( null, null, null, null, null, null, null, null, executor );

        String[] array = { "this", "is", "a", "err_weird", "test", "for", "paralleled", "err_in", "batch" };

        List<String> errors = Collections.synchronizedList( new ArrayList<>() );
        Closure closure = new Closure<String>( null )
        {
            @Override
            public String call( Object arg )
            {
                if ( ( (String) arg ).startsWith( "err_" ) )
                {
                    errors.add( (String) arg );
                }
                return null;
            }
        };

        tools.paralleledInBatch( array, 2, closure );

        assertThat( errors.size(), equalTo( 2 ) );
        assertTrue( errors.contains( "err_weird" ) );
        assertTrue( errors.contains( "err_in" ) );

    }
}
