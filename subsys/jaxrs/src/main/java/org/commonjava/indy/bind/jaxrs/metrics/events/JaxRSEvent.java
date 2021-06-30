/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.bind.jaxrs.metrics.events;

import jdk.jfr.Category;
import jdk.jfr.DataAmount;
import jdk.jfr.Description;
import jdk.jfr.Event;
import jdk.jfr.Label;
import jdk.jfr.Name;
import jdk.jfr.StackTrace;

@Name ( JaxRSEvent.NAME )
@Label ( "Invocation" )
@Category ( "JaxRS" )
@Description ( "JaxRS invocation event" )
@StackTrace ( false )
public class JaxRSEvent extends Event
{
    public static final String NAME = "o.c.i.b.j.m.e.JaxRSEvent";

    @Label ( "Resource Method" )
    public String method;

    @Label ( "Media type" )
    public String mediaType;

    @Label ( "Java method" )
    public String methodFrameName;

    @Label ( "Path" )
    public String path;

    @Label ( "Request length" )
    @DataAmount
    public int length;

    @Label ( "Response length" )
    @DataAmount
    public int responseLength;

    @Label ( "Status" )
    public int status;
}
