/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.change;

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
