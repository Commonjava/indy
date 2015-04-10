package org.commonjava.aprox.stats;

import javax.enterprise.inject.Alternative;
import javax.inject.Named;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Alternative
@Named
public class AProxVersioning
{

    private String version;

    private String builder;

    @JsonProperty( "commit-id" )
    private String commitId;

    private String timestamp;

    public AProxVersioning()
    {
    }

    @JsonCreator
    public AProxVersioning( @JsonProperty( value = "version" ) final String version,
                            @JsonProperty( "builder" ) final String builder,
                            @JsonProperty( "commit-id" ) final String commitId,
                            @JsonProperty( "timestamp" ) final String timestamp )
    {
        this.version = version;
        this.builder = builder;
        this.commitId = commitId;
        this.timestamp = timestamp;
    }

    public String getVersion()
    {
        return version;
    }

    public String getBuilder()
    {
        return builder;
    }

    public String getCommitId()
    {
        return commitId;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public void setVersion( final String version )
    {
        this.version = version;
    }

    public void setBuilder( final String builder )
    {
        this.builder = builder;
    }

    public void setCommitId( final String commitId )
    {
        this.commitId = commitId;
    }

    public void setTimestamp( final String timestamp )
    {
        this.timestamp = timestamp;
    }

}
