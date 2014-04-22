/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.depgraph.json;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.junit.Test;

public class JsonUtilsTest
{

    // ------------------------ ProjectRef -----------------------------

    @Test
    public void projectRef_ParseSinglePartGAVToProjectVersionRef()
    {
        final ProjectRef ref = JsonUtils.parseProjectRef( "org.foo:bar:1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );

        final ProjectVersionRef pvr = (ProjectVersionRef) ref;
        assertThat( pvr.getVersionString(), equalTo( "1.0" ) );
    }

    @Test
    public void projectRef_ParseMultiPartGAVToProjectVersionRef()
    {
        final ProjectRef ref = JsonUtils.parseProjectRef( "org.foo", "bar", "1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );

        final ProjectVersionRef pvr = (ProjectVersionRef) ref;
        assertThat( pvr.getVersionString(), equalTo( "1.0" ) );
    }

    @Test
    public void projectRef_ParseSinglePartGATVToArtifactRef()
    {
        final ProjectRef ref = JsonUtils.parseProjectRef( "org.foo:bar:zip:1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );

        final ProjectVersionRef pvr = (ProjectVersionRef) ref;
        assertThat( pvr.getVersionString(), equalTo( "1.0" ) );

        final ArtifactRef ar = (ArtifactRef) ref;
        assertThat( ar.getType(), equalTo( "zip" ) );
    }

    @Test
    public void projectRef_ParseMultiPartGATVToArtifactRef()
    {
        final ProjectRef ref = JsonUtils.parseProjectRef( "org.foo", "bar", "zip", "1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );

        final ProjectVersionRef pvr = (ProjectVersionRef) ref;
        assertThat( pvr.getVersionString(), equalTo( "1.0" ) );

        final ArtifactRef ar = (ArtifactRef) ref;
        assertThat( ar.getType(), equalTo( "zip" ) );
    }

    @Test
    public void projectRef_ParseSinglePartGATVCToArtifactRef()
    {
        final ProjectRef ref = JsonUtils.parseProjectRef( "org.foo:bar:zip:1.0:sources" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );

        final ProjectVersionRef pvr = (ProjectVersionRef) ref;
        assertThat( pvr.getVersionString(), equalTo( "1.0" ) );

        final ArtifactRef ar = (ArtifactRef) ref;
        assertThat( ar.getType(), equalTo( "zip" ) );
        assertThat( ar.getClassifier(), equalTo( "sources" ) );
    }

    @Test
    public void projectRef_ParseMultiPartGATVCToArtifactRef()
    {
        final ProjectRef ref = JsonUtils.parseProjectRef( "org.foo", "bar", "zip", "1.0", "sources" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );

        final ProjectVersionRef pvr = (ProjectVersionRef) ref;
        assertThat( pvr.getVersionString(), equalTo( "1.0" ) );

        final ArtifactRef ar = (ArtifactRef) ref;
        assertThat( ar.getType(), equalTo( "zip" ) );
        assertThat( ar.getClassifier(), equalTo( "sources" ) );
    }

    // ------------------------ ProjectVersionRef -----------------------------

    @Test
    public void projectVersionRef_ParseSinglePartGAVToProjectVersionRef()
    {
        final ProjectVersionRef ref = JsonUtils.parseProjectVersionRef( "org.foo:bar:1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );
        assertThat( ref.getVersionString(), equalTo( "1.0" ) );
    }

    @Test
    public void projectVersionRef_ParseMultiPartGAVToProjectVersionRef()
    {
        final ProjectVersionRef ref = JsonUtils.parseProjectVersionRef( "org.foo", "bar", "1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );
        assertThat( ref.getVersionString(), equalTo( "1.0" ) );
    }

    @Test
    public void projectVersionRef_ParseSinglePartGATVToArtifactRef()
    {
        final ProjectVersionRef ref = JsonUtils.parseProjectVersionRef( "org.foo:bar:zip:1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );
        assertThat( ref.getVersionString(), equalTo( "1.0" ) );

        final ArtifactRef ar = (ArtifactRef) ref;
        assertThat( ar.getType(), equalTo( "zip" ) );
    }

    @Test
    public void projectVersionRef_ParseMultiPartGATVToArtifactRef()
    {
        final ProjectVersionRef ref = JsonUtils.parseProjectVersionRef( "org.foo", "bar", "zip", "1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );
        assertThat( ref.getVersionString(), equalTo( "1.0" ) );

        final ArtifactRef ar = (ArtifactRef) ref;
        assertThat( ar.getType(), equalTo( "zip" ) );
    }

    @Test
    public void projectVersionRef_ParseSinglePartGATVCToArtifactRef()
    {
        final ProjectVersionRef ref = JsonUtils.parseProjectVersionRef( "org.foo:bar:zip:1.0:sources" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );
        assertThat( ref.getVersionString(), equalTo( "1.0" ) );

        final ArtifactRef ar = (ArtifactRef) ref;
        assertThat( ar.getType(), equalTo( "zip" ) );
        assertThat( ar.getClassifier(), equalTo( "sources" ) );
    }

    @Test
    public void projectVersionRef_ParseMultiPartGATVCToArtifactRef()
    {
        final ProjectVersionRef ref = JsonUtils.parseProjectVersionRef( "org.foo", "bar", "zip", "1.0", "sources" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );
        assertThat( ref.getVersionString(), equalTo( "1.0" ) );

        final ArtifactRef ar = (ArtifactRef) ref;
        assertThat( ar.getType(), equalTo( "zip" ) );
        assertThat( ar.getClassifier(), equalTo( "sources" ) );
    }

    // ------------------------ ArtifactRef -----------------------------

    @Test
    public void artifactRef_ParseSinglePartGAVIntoJarArtifactWithNoClassifier()
    {
        final ArtifactRef ref = JsonUtils.parseArtifactRef( "org.foo:bar:1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );
        assertThat( ref.getVersionString(), equalTo( "1.0" ) );
        assertThat( ref.getType(), equalTo( "jar" ) );
        assertThat( ref.getClassifier(), nullValue() );
    }

    @Test
    public void artifactRef_ParseMultiPartGAVIntoJarArtifactWithNoClassifier()
    {
        final ArtifactRef ref = JsonUtils.parseArtifactRef( "org.foo", "bar", "1.0" );
        assertThat( ref.getGroupId(), equalTo( "org.foo" ) );
        assertThat( ref.getArtifactId(), equalTo( "bar" ) );
        assertThat( ref.getVersionString(), equalTo( "1.0" ) );
        assertThat( ref.getType(), equalTo( "jar" ) );
        assertThat( ref.getClassifier(), nullValue() );
    }

}
