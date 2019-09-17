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
package org.commonjava.indy.promote.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;

/**
 * Configuration for promoting artifacts from one store to another (denoted by their corresponding {@link StoreKey}'s). If paths are provided, only
 * promote a subset of the content in the source store, otherwise promote all.
 *
 * @author jdcasey
 *
 */
public class GroupPromoteRequest
                extends AbstractPromoteRequest<GroupPromoteRequest>
{

    @ApiModelProperty( value="Indy store/repository key to promote FROM (formatted as: '{remote,hosted,group}:name')", required=true )
    private StoreKey source;

    @ApiModelProperty( value="Name of the Indy target group to promote TO (MUST be pre-existing)", required=true )
    private String targetGroup;

    @ApiModelProperty( value="Run validations, verify source and target locations ONLY, do not modify anything!" )
    private boolean dryRun;

    public GroupPromoteRequest()
    {
    }

    public GroupPromoteRequest( final StoreKey source, final String targetGroup )
    {
        this.source = source;
        this.targetGroup = targetGroup;
    }

    public StoreKey getSource()
    {
        return source;
    }

    public GroupPromoteRequest setSource( final StoreKey source )
    {
        this.source = source;
        return this;
    }

    @Override
    @JsonIgnore
    public StoreKey getTargetKey()
    {
        return new StoreKey( StoreType.group, getTargetGroup() );
    }

    public String getTargetGroup()
    {
        return targetGroup;
    }

    public GroupPromoteRequest setTargetGroup( final String targetGroup )
    {
        this.targetGroup = targetGroup;
        return this;
    }

    public boolean isDryRun()
    {
        return dryRun;
    }

    public GroupPromoteRequest setDryRun( final boolean dryRun )
    {
        this.dryRun = dryRun;
        return this;
    }

    @Override
    public boolean isFireEvents()
    {
        return false;
    }

    @Override
    public String toString()
    {
        return String.format( "GroupPromoteRequest [source=%s, target-group=%s]", source, targetGroup );
    }
}
