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
package org.commonjava.indy.pkg.npm.model;

import io.swagger.annotations.ApiModel;

import java.util.List;

@ApiModel( description = "Indicate some package commit information." )
public class Commitplease
{

    private final Boolean nohook;

    private final List<String> components;

    private final String markerPattern;

    private final String ticketPattern;

    protected Commitplease()
    {
        this.nohook = null;
        this.components = null;
        this.markerPattern = null;
        this.ticketPattern = null;
    }

    public Commitplease( final Boolean nohook, final List<String> components, final String markerPattern,
                         final String ticketPattern )
    {
        this.nohook = nohook;
        this.components = components;
        this.markerPattern = markerPattern;
        this.ticketPattern = ticketPattern;
    }

    public Boolean getNohook()
    {
        return nohook;
    }

    public List<String> getComponents()
    {
        return components;
    }

    public String getMarkerPattern()
    {
        return markerPattern;
    }

    public String getTicketPattern()
    {
        return ticketPattern;
    }
}
