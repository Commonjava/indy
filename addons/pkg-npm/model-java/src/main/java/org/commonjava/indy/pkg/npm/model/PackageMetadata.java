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
import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApiModel( description = "Specify the metadata of the relevant pulling package info." )
public class PackageMetadata
                implements Serializable, Comparable<PackageMetadata>
{
    private static final long serialVersionUID = 1L;

    private static final String TIMEOUT_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    private static final String MODIFIED = "modified";

    private static final String CREATED = "created";

    @JsonProperty( "_id" )
    private String id;

    @JsonProperty( "_rev" )
    private String rev;

    @ApiModelProperty( required = true, dataType = "String", value = "The name is what your thing is called." )
    private String name;

    private String description;

    @JsonProperty( "dist-tags" )
    private DistTag distTags = new DistTag();

    private Map<String, VersionMetadata> versions = new LinkedHashMap<String, VersionMetadata>();

    private List<UserInfo> maintainers = new ArrayList<>();

    private Map<String, String> time = new LinkedHashMap<String, String>();

    private UserInfo author;

    private Map<String, Boolean> users = new LinkedHashMap<String, Boolean>();

    @ApiModelProperty( required = false, dataType = "Repository", value = "Specify the place where your code lives." )
    private Repository repository;

    private String readme;

    private String readmeFilename;

    private String homepage;

    private List<String> keywords = new ArrayList<>();

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

    public void addMaintainers( UserInfo userInfo )
    {
        this.getMaintainers().add( userInfo );
    }

    public void addKeywords( String key )
    {
        this.getKeywords().add( key );
    }

    @Override
    public int compareTo( PackageMetadata o )
    {
        return 0;
    }

    public boolean merge( PackageMetadata source )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );

        boolean changed = false;
        if ( source.getId() != null )
        {
            this.setId( source.getId() );
            changed = true;
        }
        if ( source.getRev() != null )
        {
            this.setRev( source.getRev() );
            changed = true;
        }
        if ( source.getName() != null )
        {
            this.setName( source.getName() );
            changed = true;
        }
        if ( source.getDescription() != null )
        {
            this.setDescription( source.getDescription() );
            changed = true;
        }
        if ( source.getAuthor() != null )
        {
            this.setAuthor( source.getAuthor() );
            changed = true;
        }
        if ( source.getRepository() != null )
        {
            this.setRepository( source.getRepository() );
            changed = true;
        }
        if ( source.getReadme() != null )
        {
            this.setReadme( source.getReadme() );
            changed = true;
        }
        if ( source.getReadmeFilename() != null )
        {
            this.setReadmeFilename( source.getReadmeFilename() );
            changed = true;
        }
        if ( source.getHomepage() != null )
        {
            this.setHomepage( source.getHomepage() );
            changed = true;
        }
        if ( source.getBugs() != null )
        {
            this.setBugs( source.getBugs() );
            changed = true;
        }
        if ( source.getLicense() != null )
        {
            this.setLicense( source.getLicense() );
            changed = true;
        }
        if ( source.getAttachments() != null )
        {
            this.setAttachments( source.getAttachments() );
            changed = true;
        }

        // merge maintainers list
        Iterator maintainer = source.getMaintainers().iterator();
        while ( maintainer.hasNext() )
        {
            UserInfo m = (UserInfo) maintainer.next();
            boolean s = false;
            Iterator snapshot = maintainers.iterator();
            while ( snapshot.hasNext() )
            {
                UserInfo preExisting = (UserInfo) snapshot.next();
                if ( preExisting.getName().equals( m.getName() ) )
                {
                    s = true;
                    break;
                }
            }
            if ( !s )
            {
                this.addMaintainers( new UserInfo( m.getName(), m.getEmail(), m.getUrl() ) );
                changed = true;
            }
        }

        // merge keywords list
        Iterator keyword = source.getKeywords().iterator();
        while ( keyword.hasNext() )
        {
            String key = (String) keyword.next();
            if ( !keywords.contains( key ) )
            {
                this.addKeywords( key );
                changed = true;
            }
        }

        // merge users map
        Map<String, Boolean> sourceUsers = source.getUsers();
        for ( final String user : sourceUsers.keySet() )
        {
            if ( users.keySet().contains( user ) && users.get( user ).equals( sourceUsers.get( user ) ) )
            {
                continue;
            }
            else
            {
                users.put( user, sourceUsers.get( user ) );
                changed = true;
            }
        }

        // merge time map
        SimpleDateFormat sdf = new SimpleDateFormat( TIMEOUT_FORMAT );
        Map<String, String> sourceTimes = source.getTime();

        Iterator timeKey = sourceTimes.keySet().iterator();
        boolean added = false;

        while ( timeKey.hasNext() )
        {
            String v = (String) timeKey.next();
            String value = sourceTimes.get( v );
            boolean s = false;
            Iterator snapshot = time.keySet().iterator();

            while ( snapshot.hasNext() )
            {
                String preExisting = (String) snapshot.next();
                // when the source's version update time is more recent, will update it into the original map.
                if ( preExisting.equals( v ) )
                {
                    try
                    {
                        Date preDate = sdf.parse( time.get( v ) );
                        Date sourceDate = sdf.parse( value );
                        int compare = sourceDate.compareTo( preDate );
                        if ( compare > 0 )
                        {
                            time.put( v, value );
                            added = true;
                            changed = true;
                        }
                    }
                    catch ( ParseException e )
                    {
                        logger.error( String.format( "Cannot parse version %s 's update date. Reason: %s", preExisting,
                                                     e ) );
                    }
                    s = true;
                    break;
                }
            }

            if ( !s )
            {
                time.put( v, value );
                added = true;
                changed = true;
            }
        }

        if ( added )
        {
            List<Map.Entry<String, String>> timeList = new ArrayList<>( time.entrySet() );
            Collections.sort( timeList, ( o1, o2 ) ->
            {
                int compare = 0;
                try
                {
                    // sort the time as value (update time) asc
                    compare = sdf.parse( o1.getValue() ).compareTo( sdf.parse( o2.getValue() ) );
                }
                catch ( ParseException e )
                {
                    logger.error( String.format( "Cannot parse date when comparing. Reason: %s", e ) );
                }
                return compare;
            } );

            Map<String, String> clone = new LinkedHashMap<>();

            if ( time.get( MODIFIED ) != null )
            {
                clone.put( MODIFIED, time.get( MODIFIED ) );
            }
            if ( time.get( CREATED ) != null )
            {
                clone.put( CREATED, time.get( CREATED ) );
            }

            for ( final Map.Entry<String, String> entry : timeList )
            {
                if ( MODIFIED.equals( entry.getKey() ) || CREATED.equals( entry.getKey() ) )
                {
                    continue;
                }
                clone.put( entry.getKey(), entry.getValue() );
            }
            time = clone;
        }

        // merge dist-tag object
        DistTag sourceDist = source.getDistTags();
        Map<String, String> sourceDistMap = sourceDist.fetchTagsMap();
        Map<String, String> thisDistMap = distTags.fetchTagsMap();

        if ( sourceDistMap.size() > 0 )
        {
            if ( thisDistMap == null || thisDistMap.size() <= 0 )
            {
                this.setDistTags( sourceDist );
                changed = true;
            }
            else
            {
                for ( final String tag : sourceDistMap.keySet() )
                {
                    if ( !thisDistMap.keySet().contains( tag ) )
                    {
                        distTags.putTag( tag, sourceDistMap.get( tag ) );
                        changed = true;
                    }
                    else
                    {
                        int compare = VersionUtils.createSingleVersion( sourceDistMap.get( tag ) )
                                                  .compareTo( VersionUtils.createSingleVersion(
                                                                  thisDistMap.get( tag ) ) );
                        if ( compare > 0 )
                        {
                            distTags.putTag( tag, sourceDistMap.get( tag ) );
                            changed = true;
                        }
                    }
                }
            }
        }

        //merge versions
        Map<String, VersionMetadata> sourceVersions = source.getVersions();
        Iterator vm = sourceVersions.keySet().iterator();

        while ( vm.hasNext() )
        {
            String v = (String) vm.next();
            VersionMetadata value = sourceVersions.get( v );
            boolean s = false;
            Iterator snapshot = versions.keySet().iterator();

            while ( snapshot.hasNext() )
            {
                String preExisting = (String) snapshot.next();
                if ( preExisting.equals( v ) )
                {
                    versions.put( v, value );
                    s = true;
                    changed = true;
                    break;
                }
            }
            if ( !s )
            {
                versions.put( v, value );
                changed = true;
            }
        }

        return changed;
    }
}
