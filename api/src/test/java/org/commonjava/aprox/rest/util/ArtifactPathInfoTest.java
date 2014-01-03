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
package org.commonjava.aprox.rest.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ArtifactPathInfoTest
{

    @Test
    public void matchSnapshotUIDVersion()
    {
        final String path = "/path/to/unsigner-maven-plugin-0.2-20120307.200227-1.jar";
        assertThat( ArtifactPathInfo.isSnapshot( path ), equalTo( true ) );
    }

    @Test
    public void matchSnapshotNonUIDVersion()
    {
        final String path = "/path/to/unsigner-maven-plugin-0.2-SNAPSHOT.jar";
        assertThat( ArtifactPathInfo.isSnapshot( path ), equalTo( true ) );
    }

    @Test
    public void dontMatchNonSnapshotVersion()
    {
        final String path = "/path/to/unsigner-maven-plugin-0.2.jar";
        assertThat( ArtifactPathInfo.isSnapshot( path ), equalTo( false ) );
    }

}
