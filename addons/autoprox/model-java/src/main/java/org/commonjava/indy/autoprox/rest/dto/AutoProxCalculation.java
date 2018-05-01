/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.autoprox.rest.dto;

import java.util.Collections;
import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;

public class AutoProxCalculation
{

    @ApiModelProperty( "Main Group / repository that was created as a result of the rule firing" )
    private ArtifactStore store;

    @ApiModelProperty( "Groups / repositories that were created as a side effect of the rule firing." )
    private List<ArtifactStore> supplementalStores;

    @ApiModelProperty( "Name of the AutoProx rule that fired to create this effect." )
    private String ruleName;

    public AutoProxCalculation()
    {
    }

    public AutoProxCalculation( final RemoteRepository store, final String ruleName )
    {
        this.store = store;
        this.supplementalStores = null;
        this.ruleName = ruleName;
    }

    public AutoProxCalculation( final HostedRepository store, final String ruleName )
    {
        this.store = store;
        this.supplementalStores = null;
        this.ruleName = ruleName;
    }

    public AutoProxCalculation( final Group store, final List<ArtifactStore> supplementalStores, final String ruleName )
    {
        super();
        this.store = store;
        this.supplementalStores = supplementalStores;
        this.ruleName = ruleName;
    }

    public String getRuleName()
    {
        return ruleName;
    }

    public ArtifactStore getStore()
    {
        return store;
    }

    public List<ArtifactStore> getSupplementalStores()
    {
        return supplementalStores == null ? Collections.<ArtifactStore> emptyList() : supplementalStores;
    }

    public void setStore( final ArtifactStore store )
    {
        this.store = store;
    }

    public void setSupplementalStores( final List<ArtifactStore> supplementalStores )
    {
        this.supplementalStores = supplementalStores;
    }

    public void setRuleName( final String ruleName )
    {
        this.ruleName = ruleName;
    }

    @Override
    public boolean equals( Object o )
    {
        if ( this == o )
        {
            return true;
        }
        if ( !( o instanceof AutoProxCalculation ) )
        {
            return false;
        }

        AutoProxCalculation that = (AutoProxCalculation) o;

        if ( getStore() != null ? !getStore().equals( that.getStore() ) : that.getStore() != null )
        {
            return false;
        }
        if ( getSupplementalStores() != null ?
                !getSupplementalStores().equals( that.getSupplementalStores() ) :
                that.getSupplementalStores() != null )
        {
            return false;
        }
        return getRuleName() != null ? getRuleName().equals( that.getRuleName() ) : that.getRuleName() == null;

    }

    @Override
    public int hashCode()
    {
        int result = getStore() != null ? getStore().hashCode() : 0;
        result = 31 * result + ( getSupplementalStores() != null ? getSupplementalStores().hashCode() : 0 );
        result = 31 * result + ( getRuleName() != null ? getRuleName().hashCode() : 0 );
        return result;
    }
}
