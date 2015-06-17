/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

    private String apiVersion;

    public AProxVersioning()
    {
    }

    @JsonCreator
    public AProxVersioning( @JsonProperty( value = "version" ) final String version,
                            @JsonProperty( "builder" ) final String builder,
                            @JsonProperty( "commit-id" ) final String commitId,
                            @JsonProperty( "timestamp" ) final String timestamp,
                            @JsonProperty( "api-version" ) final String apiVersion )
    {
        this.version = version;
        this.builder = builder;
        this.commitId = commitId;
        this.timestamp = timestamp;
        this.apiVersion = apiVersion;
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

    public String getApiVersion()
    {
        return apiVersion;
    }

}
