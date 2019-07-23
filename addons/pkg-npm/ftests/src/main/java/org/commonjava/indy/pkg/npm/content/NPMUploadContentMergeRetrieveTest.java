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
package org.commonjava.indy.pkg.npm.content;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.pkg.npm.model.PackageMetadata;
import org.commonjava.indy.pkg.npm.model.UserInfo;
import org.commonjava.indy.pkg.npm.model.VersionMetadata;
import org.junit.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.commonjava.indy.pkg.npm.model.NPMPackageTypeDescriptor.NPM_PKG_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This case tests if package.json meta could be merged correctly for uploads
 * when: <br />
 * <ul>
 *      <li>creates a hosted repo</li>
 *      <li>uploads two times with same version (different metas) to the hosted repo</li>
 * </ul>
 * then: <br />
 * <ul>
 *     <li>the merged package.json can be retrieved correctly</li>
 * </ul>
 */
public class NPMUploadContentMergeRetrieveTest
        extends AbstractContentManagementTest
{
    @Test
    public void test()
            throws Exception
    {
        final InputStream content1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.5.1.json" );

        final InputStream content2 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream( "package-1.5.1-reupload.json" );

        final String path = "jquery";
        final String repoName = "test-hosted";

        HostedRepository repo = new HostedRepository( NPM_PKG_KEY, repoName );
        repo = client.stores().create( repo, "adding npm hosted repo", HostedRepository.class );
        StoreKey storeKey = repo.getKey();

        client.content().store( storeKey, path, content1 );
        client.content().store( storeKey, path, content2 );

        InputStream meta = client.content().get( storeKey, path );
        IndyObjectMapper mapper = new IndyObjectMapper( true );
        PackageMetadata merged = mapper.readValue( IOUtils.toString( meta ), PackageMetadata.class );

        // normal object fields merging verification
        assertThat( merged.getName(), equalTo( "jquery" ) );
        assertThat( merged.getAuthor().getName(), equalTo( "JS Foundation and other contributors" ) );
        assertThat( merged.getRepository().getType(), equalTo( "git" ) );
        assertThat( merged.getRepository().getUrl(), equalTo( "git+https://github.com/jquery/jquery.git" ) );
        assertThat( merged.getReadmeFilename(), equalTo( "README.md" ) );
        assertThat( merged.getHomepage(), equalTo( "https://jquery.com" ) );
        assertThat( merged.getBugs().getUrl(), equalTo( "https://github.com/jquery/jquery/issues" ) );
        assertThat( merged.getLicense(), equalTo( "MIT" ) );

        // dist-tags object merging verification
        assertThat( merged.getDistTags().getBeta(), equalTo( "3.2.1-beta.1" ) );
        assertThat( merged.getDistTags().getLatest(), equalTo( "3.2.1" ) );

        // versions map merging verification
        Map<String, VersionMetadata> versions = merged.getVersions();
        assertThat( versions, notNullValue() );
        assertThat( versions.size(), equalTo( 1 ) );
        assertThat( versions.get( "1.5.1" ).getVersion(), equalTo( "1.5.1" ) );
        assertThat( versions.get( "1.5.1" ).getDescription(), equalTo( "This is a new reupload for 1.5.1" ) );
        assertThat( versions.get( "1.5.1" ).getUrl(), equalTo( "jquery.com.new" ) );

        // maintainers list merging verification
        List<UserInfo> maintainers = merged.getMaintainers();
        assertThat( maintainers, notNullValue() );
        assertThat( maintainers.size(), equalTo( 3 ) );
        assertThat( maintainers.get( 0 ).getName(), equalTo( "dmethvin" ) );
        assertThat( maintainers.get( 1 ).getName(), equalTo( "mgol" ) );
        assertThat( maintainers.get( 2 ).getName(), equalTo( "scott.gonzalez" ) );

        // time map merging verification
        Map<String, String> times = merged.getTime();
        assertThat( times, notNullValue() );
        assertThat( times.size(), equalTo( 8 ) );
        assertThat( times.get( "modified" ), equalTo( "2017-05-23T10:57:14.309Z" ) );
        assertThat( times.get( "created" ), equalTo( "2011-04-19T07:19:56.392Z" ) );

        // users map merging verification
        Map<String, Boolean> users = merged.getUsers();
        assertThat( users, notNullValue() );
        assertThat( users.size(), equalTo( 10 ) );
        assertThat( users.get( "fgribreau" ), equalTo( true ) );

        // keywords list merging verification
        List<String> keywords = merged.getKeywords();
        assertThat( keywords, notNullValue() );
        assertThat( keywords.size(), equalTo( 4 ) );
        assertThat( keywords.contains( "javascript" ), equalTo( true ) );

        content1.close();
        content2.close();
        meta.close();
    }

    @Override
    protected boolean createStandardTestStructures()
    {
        return false;
    }
}
