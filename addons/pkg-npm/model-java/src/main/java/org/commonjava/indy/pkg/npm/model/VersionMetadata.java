/**
 * Copyright (C) 2017 Red Hat, Inc. (jdcasey@commonjava.org)
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

@ApiModel( description = "Specify all the corresponding versions metadata for the package." )
public class VersionMetadata
                implements Serializable, Comparable<VersionMetadata>
{
    private static final long serialVersionUID = 1L;

    @ApiModelProperty( required = true, dataType = "String", value = "The name and version together form an identifier that is assumed to be completely unique." )
    private String name;

    private String title;

    private String description;

    private String main;

    @ApiModelProperty( required = true, dataType = "String", value = "The name and version together form an identifier that is assumed to be completely unique." )
    private String version;

    private String url;

    private String homepage;

    private List<String> keywords;

    private UserInfo author;

    private List<UserInfo> contributors;

    private List<UserInfo> maintainers;

    @ApiModelProperty( required = false, dataType = "Map", value = "Specify the place where your code lives, keys are 'type' and 'url', 'type' values are 'git', 'svn', etc." )
    private Map<String, String> repository;

    @ApiModelProperty( required = false, dataType = "Map", value = "The 'url' to your project's issue tracker and / or the 'email' address to which issues should be reported." )
    private Map<String, String> bugs;

    @ApiModelProperty( required = false, dataType = "List", value = "Keys are 'type' and 'url', which are now deprecated. Instead, use SPDX expressions." )
    private List<Map<String, String>> licenses;

    private String license;

    private Map<String, String> dependencies;

    private Map<String, String> devDependencies;

    private Map<String, String> jsdomVersions;

    @ApiModelProperty( required = false, dataType = "Map", value = "Key 'prepare' script will be run before publishing." )
    private Map<String, String> scripts;

    private Map<String, String> dist;

    private Map<String, String> directories;

    private Map<String, Object> commitplease;

    @ApiModelProperty( required = false, dataType = "Map", value = "Specify the version of node that your stuff works on." )
    private Map<String, String> engines;

    @JsonProperty( "_engineSupported" )
    private Boolean engineSupported;

    @ApiModelProperty( required = false, dataType = "List", value = "The files will be included in your project." )
    private List<String> files;

    private String deprecated;

    private String lib;

    private String gitHead;

    @JsonProperty( "_id" )
    private String id;

    @JsonProperty( "_shasum" )
    private String shasum;

    @JsonProperty( "_from" )
    private String from;

    @JsonProperty( "_npmVersion" )
    private String npmVersion;

    @JsonProperty( "_nodeVersion" )
    private String nodeVersion;

    @JsonProperty( "_npmUser" )
    private UserInfo npmUser;

    @JsonProperty( "_npmJsonOpts" )
    private Map<String, Object> npmJsonOpts;

    @JsonProperty( "_npmOperationalInternal" )
    private Map<String, String> npmOperationalInternal;

    @JsonProperty( "_defaultsLoaded" )
    private Boolean defaultsLoaded;

    public static VersionMetadata fromString( final String id )
    {
        //TODO
        return null;
    }

    public String getName()
    {
        return name;
    }

    public void setName( String name )
    {
        this.name = name;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle( String title )
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public String getMain()
    {
        return main;
    }

    public void setMain( String main )
    {
        this.main = main;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl( String url )
    {
        this.url = url;
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

    public UserInfo getAuthor()
    {
        return author;
    }

    public void setAuthor( UserInfo author )
    {
        this.author = author;
    }

    public List<UserInfo> getContributors()
    {
        return contributors;
    }

    public void setContributors( List<UserInfo> contributors )
    {
        this.contributors = contributors;
    }

    public List<UserInfo> getMaintainers()
    {
        return maintainers;
    }

    public void setMaintainers( List<UserInfo> maintainers )
    {
        this.maintainers = maintainers;
    }

    public Map<String, String> getRepository()
    {
        return repository;
    }

    public void setRepository( Map<String, String> repository )
    {
        this.repository = repository;
    }

    public Map<String, String> getBugs()
    {
        return bugs;
    }

    public void setBugs( Map<String, String> bugs )
    {
        this.bugs = bugs;
    }

    public List<Map<String, String>> getLicenses()
    {
        return licenses;
    }

    public void setLicenses( List<Map<String, String>> licenses )
    {
        this.licenses = licenses;
    }

    public String getLicense()
    {
        return license;
    }

    public void setLicense( String license )
    {
        this.license = license;
    }

    public Map<String, String> getDependencies()
    {
        return dependencies;
    }

    public void setDependencies( Map<String, String> dependencies )
    {
        this.dependencies = dependencies;
    }

    public Map<String, String> getDevDependencies()
    {
        return devDependencies;
    }

    public void setDevDependencies( Map<String, String> devDependencies )
    {
        this.devDependencies = devDependencies;
    }

    public Map<String, String> getJsdomVersions()
    {
        return jsdomVersions;
    }

    public void setJsdomVersions( Map<String, String> jsdomVersions )
    {
        this.jsdomVersions = jsdomVersions;
    }

    public Map<String, String> getScripts()
    {
        return scripts;
    }

    public void setScripts( Map<String, String> scripts )
    {
        this.scripts = scripts;
    }

    public Map<String, String> getDist()
    {
        return dist;
    }

    public void setDist( Map<String, String> dist )
    {
        this.dist = dist;
    }

    public Map<String, String> getDirectories()
    {
        return directories;
    }

    public void setDirectories( Map<String, String> directories )
    {
        this.directories = directories;
    }

    public Map<String, Object> getCommitplease()
    {
        return commitplease;
    }

    public void setCommitplease( Map<String, Object> commitplease )
    {
        this.commitplease = commitplease;
    }

    public Map<String, String> getEngines()
    {
        return engines;
    }

    public void setEngines( Map<String, String> engines )
    {
        this.engines = engines;
    }

    public Boolean getEngineSupported()
    {
        return engineSupported;
    }

    public void setEngineSupported( Boolean engineSupported )
    {
        this.engineSupported = engineSupported;
    }

    public List<String> getFiles()
    {
        return files;
    }

    public void setFiles( List<String> files )
    {
        this.files = files;
    }

    public String getDeprecated()
    {
        return deprecated;
    }

    public void setDeprecated( String deprecated )
    {
        this.deprecated = deprecated;
    }

    public String getLib()
    {
        return lib;
    }

    public void setLib( String lib )
    {
        this.lib = lib;
    }

    public String getGitHead()
    {
        return gitHead;
    }

    public void setGitHead( String gitHead )
    {
        this.gitHead = gitHead;
    }

    public String getId()
    {
        return id;
    }

    public void setId( String id )
    {
        this.id = id;
    }

    public String getShasum()
    {
        return shasum;
    }

    public void setShasum( String shasum )
    {
        this.shasum = shasum;
    }

    public String getFrom()
    {
        return from;
    }

    public void setFrom( String from )
    {
        this.from = from;
    }

    public String getNpmVersion()
    {
        return npmVersion;
    }

    public void setNpmVersion( String npmVersion )
    {
        this.npmVersion = npmVersion;
    }

    public String getNodeVersion()
    {
        return nodeVersion;
    }

    public void setNodeVersion( String nodeVersion )
    {
        this.nodeVersion = nodeVersion;
    }

    public UserInfo getNpmUser()
    {
        return npmUser;
    }

    public void setNpmUser( UserInfo npmUser )
    {
        this.npmUser = npmUser;
    }

    public Map<String, Object> getNpmJsonOpts()
    {
        return npmJsonOpts;
    }

    public void setNpmJsonOpts( Map<String, Object> npmJsonOpts )
    {
        this.npmJsonOpts = npmJsonOpts;
    }

    public Map<String, String> getNpmOperationalInternal()
    {
        return npmOperationalInternal;
    }

    public void setNpmOperationalInternal( Map<String, String> npmOperationalInternal )
    {
        this.npmOperationalInternal = npmOperationalInternal;
    }

    public Boolean getDefaultsLoaded()
    {
        return defaultsLoaded;
    }

    public void setDefaultsLoaded( Boolean defaultsLoaded )
    {
        this.defaultsLoaded = defaultsLoaded;
    }

    @Override
    public int compareTo( VersionMetadata o )
    {
        return 0;
    }

    @Override
    public String toString()
    {
        return String.format( "\"%s\":{\"name\":%s,\"description\":%s,\"url\":%s}", version, name, description, url );
        //TODO
    }
}
