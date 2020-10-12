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
package org.commonjava.indy.promote.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.PackageTypeConstants;

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

    @ApiModelProperty( value="Indy store/repository key to promote FROM (formatted as: '{maven, npm}:{remote,hosted,group}:name')", required=true )
    private StoreKey source;

    /**
     * @deprecated As this only represents a maven type target group, will be deprecated in future. Please refer to use {@link #target} instead
     */
    @Deprecated
    @ApiModelProperty( value="Name of the Indy target group to promote TO (MUST be pre-existing)" )
    private String targetGroup;

    @ApiModelProperty( value="Indy store/repository key to promote TO (formatted as: '{maven, npm}:{hosted,group}:name')" )
    private StoreKey target;

    @ApiModelProperty( value="Run validations, verify source and target locations ONLY, do not modify anything!" )
    private boolean dryRun;

    public GroupPromoteRequest()
    {
    }

    /**
     * @param source
     * @param targetGroup
     * @deprecated As this constructor only works for maven type target group, will be deprecated in future. Please use {@link #GroupPromoteRequest(StoreKey, StoreKey)} instead
     */
    @Deprecated
    public GroupPromoteRequest( final StoreKey source, final String targetGroup )
    {
        this.source = source;
        this.targetGroup = targetGroup;
        this.target = new StoreKey( PackageTypeConstants.PKG_TYPE_MAVEN, StoreType.group, targetGroup );
    }

    public GroupPromoteRequest( final StoreKey source, final StoreKey target )
    {
        this.source = source;
        this.target = target;
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
        return getTarget();
    }

    /**
     * @deprecated As this only returns a maven typed target group, it will be deprecated in future. Please use {@link #getTarget()} instead
     */
    @Deprecated
    public String getTargetGroup()
    {
        return targetGroup;
    }

    /**
     * @deprecated As this only creates a maven typed target group, it will be deprecated in future. Please use {@link #setTarget(StoreKey)} instead
     */
    @Deprecated
    public GroupPromoteRequest setTargetGroup( final String targetGroup )
    {
        this.targetGroup = targetGroup;
        this.target = StoreKey.fromString( "maven:group:" + targetGroup );
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

    public StoreKey getTarget()
    {
        if ( target == null )
        {
            target = new StoreKey( PackageTypeConstants.PKG_TYPE_MAVEN, StoreType.group, getTargetGroup() );
        }
        return target;
    }

    public GroupPromoteRequest setTarget( StoreKey target )
    {
        this.target = target;
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
