/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PackagePathTest
{
    @Test
    public void testNormal()
    {
        Optional<PackagePath> packagePath = PackagePath.parse( "/jquery/-/jquery-1.2.3-redhat-1.tgz" );
        assertTrue( packagePath.isPresent() );
        PackagePath path = packagePath.get();
        assertResult( path, "jquery", null, "jquery/1.2.3-redhat-1", "1.2.3-redhat-1" );

        packagePath = PackagePath.parse( "/jquery/-/jquery-1.2.3-redhat-1.tgz" );
        assertTrue( packagePath.isPresent() );
        path = packagePath.get();
        assertResult( path, "jquery", null, "jquery/1.2.3-redhat-1", "1.2.3-redhat-1" );
    }

    @Test
    public void testScoped()
    {
        Optional<PackagePath> packagePath = PackagePath.parse( "/@angular/core/-/core-1.2.3-redhat-1.tgz" );
        assertTrue( packagePath.isPresent() );
        PackagePath path = packagePath.get();
        assertResult( path, "core", "@angular", "@angular/core/1.2.3-redhat-1", "1.2.3-redhat-1" );

        packagePath = PackagePath.parse( "@angular/core/-/core-1.2.3-redhat-1.tgz" );
        assertTrue( packagePath.isPresent() );
        path = packagePath.get();
        assertResult( path, "core", "@angular", "@angular/core/1.2.3-redhat-1", "1.2.3-redhat-1" );
    }

    @Test
    public void testNormalVersionPath()
    {
        Optional<PackagePath> packagePath = PackagePath.parse( "/keycloak-connect/8.0.0-rc.2" );
        assertTrue( packagePath.isPresent() );
        PackagePath path = packagePath.get();
        assertResult( path, "keycloak-connect", null, "keycloak-connect/8.0.0-rc.2", "8.0.0-rc.2" );

        packagePath = PackagePath.parse( "keycloak-connect/8.0.0-rc.2" );
        assertTrue( packagePath.isPresent() );
        path = packagePath.get();
        assertResult( path, "keycloak-connect", null, "keycloak-connect/8.0.0-rc.2", "8.0.0-rc.2" );
    }

    @Test
    public void testScopedVersionPath()
    {
        Optional<PackagePath> packagePath = PackagePath.parse( "/@redhat/keycloak-connect/8.0.0-rc.2" );
        assertTrue( packagePath.isPresent() );
        PackagePath path = packagePath.get();
        assertResult( path, "keycloak-connect", "@redhat", "@redhat/keycloak-connect/8.0.0-rc.2", "8.0.0-rc.2" );

        packagePath = PackagePath.parse( "/@redhat/keycloak-connect/8.0.0-rc.2" );
        assertTrue( packagePath.isPresent() );
        path = packagePath.get();
        assertResult( path, "keycloak-connect", "@redhat", "@redhat/keycloak-connect/8.0.0-rc.2", "8.0.0-rc.2" );
    }

    private void assertResult( final PackagePath packagePath, final String expectedPackage, final String expectedScope,
                               final String expectVersionPath, final String expectVersion )
    {
        assertThat( packagePath.isScoped(), equalTo( expectedScope != null ) );
        assertThat( packagePath.getPackageName(), equalTo( expectedPackage ) );
        if ( packagePath.isScoped() )
        {
            assertThat( packagePath.getScopedName(), equalTo( expectedScope ) );
        }
        assertThat( packagePath.getVersionPath(), equalTo( expectVersionPath ) );
        assertThat( packagePath.getVersion(), equalTo( expectVersion ) );
    }
}
