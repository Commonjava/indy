package org.commonjava.indy.cassandra.data;

import com.datastax.driver.mapping.annotations.ClusteringColumn;
import com.datastax.driver.mapping.annotations.Column;
import com.datastax.driver.mapping.annotations.PartitionKey;
import com.datastax.driver.mapping.annotations.Table;

import java.util.Map;
import java.util.Set;

@Table( name = "artifactstore", readConsistency = "QUORUM", writeConsistency = "QUORUM" )
public class DtxArtifactStore
{

    @PartitionKey(0)
    private String packageType;

    @PartitionKey(1)
    private String storeType;

    @ClusteringColumn
    private String name;

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

    public String getPackageType()
    {
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
        return "DtxArtifactStore{" + "packageType='" + packageType + '\'' + ", storeType='" + storeType + '\''
                        + ", name='" + name + '\'' + ", description='" + description + '\'' + ", transientMetadata="
                        + transientMetadata + ", metadata=" + metadata + ", disabled=" + disabled + ", disableTimeout="
                        + disableTimeout + ", pathStyle='" + pathStyle + '\'' + ", pathMaskPatterns=" + pathMaskPatterns
                        + ", authoritativeIndex=" + authoritativeIndex + ", createTime='" + createTime + '\''
                        + ", rescanInProgress=" + rescanInProgress + ", extras=" + extras + '}';
    }
}
