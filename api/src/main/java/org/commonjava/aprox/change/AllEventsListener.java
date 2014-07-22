/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.change;

/** Debugging event listener to simply spit each event out to the system console. */
@javax.enterprise.context.ApplicationScoped
public class AllEventsListener
{

    /** Print the event to the console */
    public void onEvent( /*@Observes @Any*/final Object evt )
    {
        System.out.printf( "\n\n\n\n[ALL] {}\n\n\n\n", evt );
    }

}
