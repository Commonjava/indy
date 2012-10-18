package org.commonjava.aprox.change;


@javax.enterprise.context.ApplicationScoped
public class AllEventsListener
{

    public void onEvent( /*@Observes @Any*/final Object evt )
    {
        System.out.printf( "\n\n\n\n[ALL] %s\n\n\n\n", evt );
    }

}
