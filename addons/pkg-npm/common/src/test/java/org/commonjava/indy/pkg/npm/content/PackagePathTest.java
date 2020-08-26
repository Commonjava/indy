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

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class PackagePathTest
{
    @Test
    public void testNormal()
    {
        Optional<PackagePath> packagePath = PackagePath.parse( "jquery/-/jquery-1.2.3-redhat-1.tgz" );
        assertTrue( packagePath.isPresent() );
        PackagePath path = packagePath.get();
        assertFalse( path.isScoped() );
        assertThat( path.getPackageName(), equalTo( "jquery" ) );
        assertThat( path.getVersionPath(), equalTo( "jquery/1.2.3-redhat-1" ) );
        assertThat( path.getVersion(), equalTo( "1.2.3-redhat-1" ) );
        packagePath = PackagePath.parse( "jquery/jquery-1.2.3-redhat-1.tgz" );
        assertFalse( packagePath.isPresent() );
    }

    @Test
    public void testScoped()
    {
        Optional<PackagePath> packagePath = PackagePath.parse( "@angular/core/-/core-1.2.3-redhat-1.tgz" );
        assertTrue( packagePath.isPresent() );
        PackagePath path = packagePath.get();
        assertTrue( path.isScoped() );
        assertThat( path.getPackageName(), equalTo( "core" ) );
        assertThat( path.getScopedName(), equalTo( "@angular" ) );
        assertThat( path.getVersionPath(), equalTo( "@angular/core/1.2.3-redhat-1" ) );
        assertThat( path.getVersion(), equalTo( "1.2.3-redhat-1" ) );
        packagePath = PackagePath.parse( "@angular/core/jquery-1.2.3-redhat-1.tgz" );
        assertFalse( packagePath.isPresent() );
    }
}
