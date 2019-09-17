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
package org.commonjava.indy.koji.model;

import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.model.core.StoreKey;

/**
 * Request to repair Koji remote stores. If source is a group, all repositories in the group are to be repaired.
 *
 * @author ruhan
 *
 */
public class KojiRepairRequest
{
    @ApiModelProperty( value = "Koji repository key to repair (formatted as: '{maven}:{remote,group}:name')", required = true )
    private StoreKey source;

    @ApiModelProperty( value = "Repair arguments" )
    private String args;

    @ApiModelProperty( value = "Get repair report ONLY, not modify anything." )
    private boolean dryRun;

    public KojiRepairRequest() {}

    public KojiRepairRequest( final StoreKey source, boolean dryRun )
    {
        this.source = source;
        this.dryRun = dryRun;
    }

    public StoreKey getSource()
    {
        return source;
    }

    public void setSource( final StoreKey source )
    {
        this.source = source;
    }

    public String getArgs()
    {
        return args;
    }

    public void setArgs( String args )
    {
        this.args = args;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public void setDryRun( final boolean dryRun )
    {
        this.dryRun = dryRun;
    }

}
