package org.commonjava.indy.promote.validate;

import groovy.lang.Closure;
import org.commonjava.indy.promote.conf.PromoteConfig;
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
    final String[] array = { "this", "is", "a", "err_weird", "test", "err_for", "paralleled", "err_in", "batch" };

    @Test
    public void testParalleledInBatch()
    {
        PromoteConfig config = new PromoteConfig();
        Executor executor = Executors.newCachedThreadPool();
        PromotionValidationTools tools =
                        new PromotionValidationTools( null, null, null, null, null, null, null, null, executor,
                                                      config );

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

        tools.paralleledInBatch( array, closure );
        verifyIt( errors );
    }

    @Test
    public void testParalleledInBatch_smallSize()
    {
        PromoteConfig config = new PromoteConfig();
        config.setParalleledBatchSize( 2 );

        Executor executor = Executors.newCachedThreadPool();
        PromotionValidationTools tools =
                        new PromotionValidationTools( null, null, null, null, null, null, null, null, executor,
                                                      config );

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

        tools.paralleledInBatch( array, closure );
        verifyIt( errors );
    }

    private void verifyIt( List<String> errors )
    {
        assertThat( errors.size(), equalTo( 3 ) );
        assertTrue( errors.contains( "err_weird" ) );
        assertTrue( errors.contains( "err_for" ) );
        assertTrue( errors.contains( "err_in" ) );
    }
}
