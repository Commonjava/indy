package org.commonjava.aprox.stats;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class AProxVersioning
{

    private final String version;

    private final String builder;

    @JsonProperty( "commit-id" )
    private final String commitId;

    private final String timestamp;

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

}
