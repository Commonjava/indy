/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.sli.jaxrs;

import org.commonjava.indy.sli.metrics.GoldenSignalsMetricSet;
import org.commonjava.maven.galley.io.SpecialPathManagerImpl;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class GoldenSignalsFilterTest
{
    private final SpecialPathManagerImpl specialPathManager = new SpecialPathManagerImpl();

    private final GoldenSignalsMetricSet metricSet = new GoldenSignalsMetricSet();

    private final GoldenSignalsFilter filter = new GoldenSignalsFilter( metricSet, specialPathManager );

    @Before
    public void init()
    {
        specialPathManager.initPkgPathSets();
    }

    @Test
    public void testIsMetadata()
    {
        final boolean isFoloMavenMetadata = filter.isMetadata( "maven", "group", "test",
                                                               new String[] { "", "folo", "track", "test-1", "maven",
                                                                       "group", "test", "io", "undertow",
                                                                       "maven-metadata.xml" }, 7 );
        assertThat( isFoloMavenMetadata, CoreMatchers.equalTo( true ) );

        final boolean isContentMavenMetadata = filter.isMetadata( "maven", "group", "test",
                                                                  new String[] { "", "content", "maven", "group",
                                                                          "test", "io", "undertow",
                                                                          "maven-metadata.xml" }, 5 );
        assertThat( isContentMavenMetadata, CoreMatchers.equalTo( true ) );

        final boolean isDeprecatedMavenMetadata = filter.isMetadata( "maven", "group", "test",
                                                                     new String[] { "", "group", "test", "io",
                                                                             "undertow", "maven-metadata.xml" }, 2 );
        assertThat( isDeprecatedMavenMetadata, CoreMatchers.equalTo( true ) );

        final boolean isFoloNPMMetadata = filter.isMetadata( "npm", "group", "test",
                                                             new String[] { "", "folo", "track", "test-1", "maven",
                                                                     "group", "test", "jquery", "package.json" }, 7 );
        assertThat( isFoloNPMMetadata, CoreMatchers.equalTo( true ) );

        final boolean isContentNPMMetadata = filter.isMetadata( "npm", "group", "test",
                                                                new String[] { "", "content", "npm", "group", "test",
                                                                        "jquery", "package.json" }, 5 );
        assertThat( isContentNPMMetadata, CoreMatchers.equalTo( true ) );
    }

    @Test
    public void testIsNormalPath()
    {
        final boolean isFoloMavenPath = filter.isMetadata( "maven", "group", "test",
                                                           new String[] { "", "folo", "track", "test-1", "maven",
                                                                   "group", "test", "io", "undertow", "1.0",
                                                                   "undertow-1.0.pom" }, 7 );
        assertThat( isFoloMavenPath, CoreMatchers.equalTo( false ) );

        final boolean isContentMavenPath = filter.isMetadata( "maven", "group", "test",
                                                              new String[] { "", "content", "maven", "group", "test",
                                                                      "io", "undertow", "1.0", "undertow-1.0.pom" },
                                                              5 );
        assertThat( isContentMavenPath, CoreMatchers.equalTo( false ) );

        final boolean isDeprecatedMavenPath = filter.isMetadata( "maven", "group", "test",
                                                                 new String[] { "", "group", "test", "io", "undertow",
                                                                         "maven-metadata.xml" }, 2 );
        assertThat( isDeprecatedMavenPath, CoreMatchers.equalTo( true ) );

        final boolean isFoloNPMPath = filter.isMetadata( "npm", "group", "test",
                                                         new String[] { "", "folo", "track", "test-1", "maven", "group",
                                                                 "test", "jquery", "-", "jquery.tar.gz" }, 7 );
        assertThat( isFoloNPMPath, CoreMatchers.equalTo( false ) );

        final boolean isContentNPMPath = filter.isMetadata( "npm", "group", "test",
                                                            new String[] { "", "content", "npm", "group", "test",
                                                                    "jquery", "-", "jquery.tar.gz" }, 5 );
        assertThat( isContentNPMPath, CoreMatchers.equalTo( false ) );
    }

    @Test
    public void testGetFunctions(){
        List<String> groupPromoteFuncs= filter.getFunctions( "/promotion/groups/promote", "" );
        assertThat( groupPromoteFuncs, hasItem( GoldenSignalsMetricSet.FN_PROMOTION ) );
        List<String> pathsPromoteFuncs= filter.getFunctions( "/promotion/paths/promote", "" );
        assertThat( groupPromoteFuncs, hasItem( GoldenSignalsMetricSet.FN_PROMOTION ) );

        List<String> storesFuncs = filter.getFunctions( "/admin/stores/maven/group/test", "PUT" );
        assertThat( storesFuncs, hasItem( GoldenSignalsMetricSet.FN_REPO_MGMT ) );
        storesFuncs = filter.getFunctions( "/admin/stores/maven/remote/test2", "POST" );
        assertThat( storesFuncs, hasItem( GoldenSignalsMetricSet.FN_REPO_MGMT ) );
        storesFuncs = filter.getFunctions( "/admin/stores/maven/hosted/test3", "DELETE" );
        assertThat( storesFuncs, hasItem( GoldenSignalsMetricSet.FN_REPO_MGMT ) );

        List<String> listingFuncs = filter.getFunctions( "/browse/maven/group/test", "" );
        assertThat( listingFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_LISTING ) );
        listingFuncs = filter.getFunctions( "/content/maven/group/test/", "" );
        assertThat( listingFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_LISTING ) );
        listingFuncs = filter.getFunctions( "/content/maven/group/test", "" );
        assertThat( listingFuncs, not(hasItem( GoldenSignalsMetricSet.FN_CONTENT_LISTING )) );
        listingFuncs = filter.getFunctions( "/content/maven/group/test/index.html", "" );
        assertThat( listingFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_LISTING ) );
        listingFuncs = filter.getFunctions( "/group/test/", "" );
        assertThat( listingFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_LISTING ) );
        listingFuncs = filter.getFunctions( "/group/test/index.html", "" );
        assertThat( listingFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_LISTING ) );

        List<String> foloFuncs = filter.getFunctions( "/folo/admin/test-1/record", "" );
        assertThat( foloFuncs, hasItem( GoldenSignalsMetricSet.FN_TRACKING_RECORD ) );
        foloFuncs = filter.getFunctions( "/folo/admin/test-1/report", "" );
        assertThat( foloFuncs, hasItem( GoldenSignalsMetricSet.FN_TRACKING_RECORD ) );

        List<String> contentMavenFoloFuncs = filter.getFunctions( "/folo/track/test-1/maven/group/test/foo/bar/1.0/bar-1.0.pom", "" );
        assertThat( contentMavenFoloFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_MAVEN ) );
        List<String> contentMavenMetaFoloFuncs = filter.getFunctions( "/folo/track/test-1/maven/group/test/foo/bar/maven-metadata.xml", "" );
        assertThat( contentMavenMetaFoloFuncs, hasItem( GoldenSignalsMetricSet.FN_METADATA_MAVEN ) );
        List<String> contentNPMFoloFuncs = filter.getFunctions( "/folo/track/test-1/npm/group/test/foo/-/foo.tar.gz", "" );
        assertThat( contentNPMFoloFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_NPM ) );
        List<String> contentNPMMetaFoloFuncs = filter.getFunctions( "/folo/track/test-1/npm/group/test/foo/package.json", "" );
        assertThat( contentNPMMetaFoloFuncs, hasItem( GoldenSignalsMetricSet.FN_METADATA_NPM ) );

        List<String> contentMavenFuncs = filter.getFunctions( "/content/maven/group/test/foo/bar/1.0/bar-1.0.pom", "" );
        assertThat( contentMavenFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_MAVEN ) );
        List<String> contentMavenMetaFuncs = filter.getFunctions( "/content/maven/group/test/foo/bar/maven-metadata.xml", "" );
        assertThat( contentMavenMetaFuncs, hasItem( GoldenSignalsMetricSet.FN_METADATA_MAVEN ) );
        List<String> contentNPMFuncs = filter.getFunctions( "/content/npm/group/test/foo/-/foo.tar.gz", "" );
        assertThat( contentNPMFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_NPM ) );
        List<String> contentNPMMetaFuncs = filter.getFunctions( "/content/npm/group/test/foo/package.json", "" );
        assertThat( contentNPMMetaFuncs, hasItem( GoldenSignalsMetricSet.FN_METADATA_NPM ) );

        List<String> contentMavenDepFuncs = filter.getFunctions( "/group/test/foo/bar/1.0/bar-1.0.pom", "" );
        assertThat( contentMavenDepFuncs, hasItem( GoldenSignalsMetricSet.FN_CONTENT_MAVEN ) );
        List<String> contentMavenMetaDepFuncs = filter.getFunctions( "/group/test/foo/bar/maven-metadata.xml", "" );
        assertThat( contentMavenMetaDepFuncs, hasItem( GoldenSignalsMetricSet.FN_METADATA_MAVEN ) );
    }

}
