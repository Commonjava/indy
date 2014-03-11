/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

public class ArtifactPathInfo
{

    // regex developed at: http://fiddle.re/tvk5
    private static final String ARTIFACT_PATH_REGEX =
        "\\/?(([^\\/]+\\/)*[^\\/]+)\\/([^\\/]+)\\/([^\\/]+)\\/(\\3-((\\4)|(.+-[0-9]{8}\\.[0-9]{6}-[0-9]+))(-([^.]+))?(\\.(.+)))";

    private static final int GROUP_ID_GROUP = 1;

    private static final int ARTIFACT_ID_GROUP = 3;

    private static final int FILE_GROUP = 5;

    private static final int VERSION_GROUP = 6;

    private static final int CLASSIFIER_GROUP = 10;

    private static final int TYPE_GROUP = 12;

    public static ArtifactPathInfo parse( final String path )
    {
        if ( path == null )
        {
            return null;
        }

        final Matcher matcher = Pattern.compile( ARTIFACT_PATH_REGEX )
                                       .matcher( path.replace( '\\', '/' ) );
        if ( !matcher.matches() )
        {
            return null;
        }

        final String g = matcher.group( GROUP_ID_GROUP )
                                .replace( '/', '.' );
        final String a = matcher.group( ARTIFACT_ID_GROUP );
        final String v = matcher.group( VERSION_GROUP );
        final String c = matcher.group( CLASSIFIER_GROUP );
        final String t = matcher.group( TYPE_GROUP );
        final String f = matcher.group( FILE_GROUP );

        return new ArtifactPathInfo( g, a, v, c, t, f, path );
    }

    private final String groupId;

    private final String artifactId;

    private final String version;

    private final String classifier;

    private final String type;

    private final String file;

    private final String fullPath;

    private final boolean isSnapshot;

    public ArtifactPathInfo( final String groupId, final String artifactId, final String version, final String file, final String fullPath )
    {
        this( groupId, artifactId, version, null, "jar", file, fullPath );
    }

    public ArtifactPathInfo( final String groupId, final String artifactId, final String version, final String classifier, final String type,
                             final String file, final String fullPath )
    {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.classifier = classifier;
        this.type = type;
        this.file = file;
        this.fullPath = fullPath;
        isSnapshot = isSnapshot( fullPath );
    }

    public boolean isSnapshot()
    {
        return isSnapshot;
    }

    private SnapshotInfo snapshotInfo;

    public synchronized SnapshotInfo getSnasphotInfo()
    {
        if ( snapshotInfo == null && isSnapshot )
        {
            snapshotInfo = parseSnapshotInfo( version );
        }

        return snapshotInfo;
    }

    public String getGroupId()
    {
        return groupId;
    }

    public String getArtifactId()
    {
        return artifactId;
    }

    public String getVersion()
    {
        return version;
    }

    public String getClassifier()
    {
        return classifier;
    }

    public String getType()
    {
        return type;
    }

    public String getFile()
    {
        return file;
    }

    public String getFullPath()
    {
        return fullPath;
    }

    @Override
    public String toString()
    {
        return String.format( "ArtifactPathInfo [groupId=%s, artifactId=%s, version=%s, file=%s]", groupId, artifactId, version, file );
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( artifactId == null ) ? 0 : artifactId.hashCode() );
        result = prime * result + ( ( file == null ) ? 0 : file.hashCode() );
        result = prime * result + ( ( groupId == null ) ? 0 : groupId.hashCode() );
        result = prime * result + ( ( version == null ) ? 0 : version.hashCode() );
        return result;
    }

    @Override
    public boolean equals( final Object obj )
    {
        if ( this == obj )
        {
            return true;
        }
        if ( obj == null )
        {
            return false;
        }
        if ( getClass() != obj.getClass() )
        {
            return false;
        }
        final ArtifactPathInfo other = (ArtifactPathInfo) obj;
        if ( artifactId == null )
        {
            if ( other.artifactId != null )
            {
                return false;
            }
        }
        else if ( !artifactId.equals( other.artifactId ) )
        {
            return false;
        }
        if ( file == null )
        {
            if ( other.file != null )
            {
                return false;
            }
        }
        else if ( !file.equals( other.file ) )
        {
            return false;
        }
        if ( groupId == null )
        {
            if ( other.groupId != null )
            {
                return false;
            }
        }
        else if ( !groupId.equals( other.groupId ) )
        {
            return false;
        }
        if ( version == null )
        {
            if ( other.version != null )
            {
                return false;
            }
        }
        else if ( !version.equals( other.version ) )
        {
            return false;
        }
        return true;
    }

    private static final String UNIQUE_SNAPSHOT_PATTERN = ".+-(\\d\\d\\d\\d\\d\\d\\d\\d\\.\\d\\d\\d\\d\\d\\d)-(\\d+)(\\..+)?";

    private static final String NON_UNIQUE_SNAPSHOT_PATTERN = ".+-SNAPSHOT(\\..+)?";

    public static boolean isSnapshot( final String path )
    {
        return path.matches( UNIQUE_SNAPSHOT_PATTERN ) || path.matches( NON_UNIQUE_SNAPSHOT_PATTERN );
    }

    public static SnapshotInfo parseSnapshotInfo( final String version )
    {
        final Matcher matcher = Pattern.compile( UNIQUE_SNAPSHOT_PATTERN )
                                       .matcher( version );
        if ( matcher.matches() )
        {
            return new SnapshotInfo( matcher.group( 1 ), Integer.parseInt( matcher.group( 2 ) ) );
        }

        return null;
    }

    public static final class SnapshotInfo
    {
        private final String timestamp;

        private final int buildNumber;

        private SnapshotInfo( final String timestamp, final int buildNumber )
        {
            this.timestamp = timestamp;
            this.buildNumber = buildNumber;
        }

        public String getTimestamp()
        {
            return timestamp;
        }

        public int getBuildNumber()
        {
            return buildNumber;
        }
    }

    public ProjectVersionRef getProjectId()
    {
        return new ProjectVersionRef( getGroupId(), getArtifactId(), getVersion() );
    }

}
