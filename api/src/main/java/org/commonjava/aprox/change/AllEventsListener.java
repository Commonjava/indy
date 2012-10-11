package org.commonjava.aprox.change;

import javax.inject.Singleton;

@Singleton
public class AllEventsListener
{

    public void onEvent( /*@Observes @Any*/final Object evt )
    {
        System.out.printf( "\n\n\n\n[ALL] %s\n\n\n\n", evt );
    }

}
