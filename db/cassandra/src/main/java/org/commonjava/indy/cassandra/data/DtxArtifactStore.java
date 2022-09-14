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
package org.commonjava.indy.cassandra.data;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Map;
import java.util.Set;

import static org.commonjava.indy.cassandra.data.CassandraStoreUtil.TABLE_STORE;

/**
 * @deprecated The store management functions has been extracted into Repository Service, which is maintained in "ServiceStoreDataManager"
 */
@Deprecated
@Table( name = TABLE_STORE, readConsistency = "QUORUM", writeConsistency = "QUORUM" )
public class DtxArtifactStore
{

    @PartitionKey(0)
    private String typeKey;

    @PartitionKey(1)
    private Integer nameHashPrefix;

    @ClusteringColumn
    private String name;

    @Column
    private String packageType;

    @Column
    private String storeType;

    @Column
    private String description;

    @Column
    private transient Map<String, String> transientMetadata;

    @Column
    private Map<String, String> metadata;

    @Column
    private boolean disabled;

    @Column
    private int disableTimeout;

    @Column
    private String pathStyle;

    @Column
    private Set<String> pathMaskPatterns;

    @Column
    private Boolean authoritativeIndex;

    @Column
    private String createTime;

    @Column
    private Boolean rescanInProgress = false;

    /**
     * The map holds the specific attributes for each type of repository
     */
    @Column
    private Map<String, String> extras;

    public String getTypeKey() { return typeKey; }

    public void setTypeKey( String typeKey ) { this.typeKey = typeKey; }

    public Integer getNameHashPrefix() { return nameHashPrefix; }

    public void setNameHashPrefix( Integer nameHashPrefix ) { this.nameHashPrefix = nameHashPrefix; }

    public String getPackageType() {
        return packageType;
    }

    public void setPackageType( String packageType )
    {
        this.packageType = packageType;
    }

    public String getStoreType()
    {
        return storeType;
    }

    public void setStoreType( String storeType )
    {
        this.storeType = storeType;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public Map<String, String> getTransientMetadata()
    {
        return transientMetadata;
    }

    public void setTransientMetadata( Map<String, String> transientMetadata )
    {
        this.transientMetadata = transientMetadata;
    }

    public Map<String, String> getMetadata()
    {
        return metadata;
    }

    public void setMetadata( Map<String, String> metadata )
    {
        this.metadata = metadata;
    }

    public boolean isDisabled()
    {
        return disabled;
    }

    public void setDisabled( boolean disabled )
    {
        this.disabled = disabled;
    }

    public int getDisableTimeout()
    {
        return disableTimeout;
    }

    public void setDisableTimeout( int disableTimeout )
    {
        this.disableTimeout = disableTimeout;
    }

    public String getPathStyle()
    {
        return pathStyle;
    }

    public void setPathStyle( String pathStyle )
    {
        this.pathStyle = pathStyle;
    }

    public Set<String> getPathMaskPatterns()
    {
        return pathMaskPatterns;
    }

    public void setPathMaskPatterns( Set<String> pathMaskPatterns )
    {
        this.pathMaskPatterns = pathMaskPatterns;
    }

    public Boolean getAuthoritativeIndex()
    {
        return authoritativeIndex;
    }

    public void setAuthoritativeIndex( Boolean authoritativeIndex )
    {
        this.authoritativeIndex = authoritativeIndex;
    }

    public String getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime( String createTime )
    {
        this.createTime = createTime;
    }

    public Boolean getRescanInProgress()
    {
        return rescanInProgress;
    }

    public void setRescanInProgress( Boolean rescanInProgress )
    {
        this.rescanInProgress = rescanInProgress;
    }

    public Map<String, String> getExtras()
    {
        return extras;
    }

    public void setExtras( Map<String, String> extras )
    {
        this.extras = extras;
    }

    @Override
    public String toString()
    {
        return "DtxArtifactStore{" + "typeKey='" + typeKey + '\'' + ", nameHashPrefix=" + nameHashPrefix + ", name='"
                        + name + '\'' + ", packageType='" + packageType + '\'' + ", storeType='" + storeType + '\''
                        + ", description='" + description + '\'' + ", transientMetadata=" + transientMetadata
                        + ", metadata=" + metadata + ", disabled=" + disabled + ", disableTimeout=" + disableTimeout
                        + ", pathStyle='" + pathStyle + '\'' + ", pathMaskPatterns=" + pathMaskPatterns
                        + ", authoritativeIndex=" + authoritativeIndex + ", createTime='" + createTime + '\''
                        + ", rescanInProgress=" + rescanInProgress + ", extras=" + extras + '}';
    }
}
