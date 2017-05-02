/**
 * Copyright (C) 2017 Red Hat, Inc. (yma@commonjava.org)
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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@ApiModel( description = "Specify the metadata of the relevant pulling package info." )
public class PackageMetadata
                implements Serializable, Comparable<PackageMetadata>
{
    private static final long serialVersionUID = 1L;

    @JsonProperty( "_id" )
    private String id;

    @JsonProperty( "_rev" )
    private String rev;

    @ApiModelProperty( required = true, dataType = "String", value = "The name is what your thing is called." )
    private String name;

    private String description;

    @JsonProperty( "dist-tags" )
    private DistTag distTags;

    private Map<String, VersionMetadata> versions;

    private List<UserInfo> maintainers;

    private Map<String, String> time;

    private UserInfo author;

    private Map<String, Boolean> users;

    @ApiModelProperty( required = false, dataType = "Repository", value = "Specify the place where your code lives." )
    private Repository repository;

    private String readme;

    private String readmeFilename;

    private String homepage;

    private List<String> keywords;

    @ApiModelProperty( required = false, dataType = "Bugs", value = "The issue tracker and / or the email address to which issues should be reported." )
    private Bugs bugs;

    private String license;

    @JsonProperty( "_attachments" )
    private Object attachments;

    public PackageMetadata()
    {
    }

    public PackageMetadata( String name )
    {
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getRev()
    {
        return rev;
    }

    public void setRev( String rev )
    {
        this.rev = rev;
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

    public DistTag getDistTags()
    {
        return distTags;
    }

    public void setDistTags( DistTag distTags )
    {
        this.distTags = distTags;
    }

    public Map<String, VersionMetadata> getVersions()
    {
        return versions;
    }

    public void setVersions( Map<String, VersionMetadata> versions )
    {
        this.versions = versions;
    }

    public List<UserInfo> getMaintainers()
    {
        return maintainers;
    }

    public void setMaintainers( List<UserInfo> maintainers )
    {
        this.maintainers = maintainers;
    }

    public Map<String, String> getTime()
    {
        return time;
    }

    public void setTime( Map<String, String> time )
    {
        this.time = time;
    }

    public UserInfo getAuthor()
    {
        return author;
    }

    public void setAuthor( UserInfo author )
    {
        this.author = author;
    }

    public Map<String, Boolean> getUsers()
    {
        return users;
    }

    public void setUsers( Map<String, Boolean> users )
    {
        this.users = users;
    }

    public Repository getRepository()
    {
        return repository;
    }

    public void setRepository( Repository repository )
    {
        this.repository = repository;
    }

    public String getReadme()
    {
        return readme;
    }

    public void setReadme( String readme )
    {
        this.readme = readme;
    }

    public String getReadmeFilename()
    {
        return readmeFilename;
    }

    public void setReadmeFilename( String readmeFilename )
    {
        this.readmeFilename = readmeFilename;
    }

    public String getHomepage()
    {
        return homepage;
    }

    public void setHomepage( String homepage )
    {
        this.homepage = homepage;
    }

    public List<String> getKeywords()
    {
        return keywords;
    }

    public void setKeywords( List<String> keywords )
    {
        this.keywords = keywords;
    }

    public Bugs getBugs()
    {
        return bugs;
    }

    public void setBugs( Bugs bugs )
    {
        this.bugs = bugs;
    }

    public String getLicense()
    {
        return license;
    }

    public void setLicense( String license )
    {
        this.license = license;
    }

    public Object getAttachments()
    {
        return attachments;
    }

    public void setAttachments( Object attachments )
    {
        this.attachments = attachments;
    }

    @Override
    public int compareTo( PackageMetadata o )
    {
        return 0;
    }
}
