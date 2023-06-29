/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.io.SpecialPathManagerImpl;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class NPMStoragePathCalculatorTest
{

    @Test
    public void testNormal()
    {
        NPMStoragePathCalculator pathCalculator = new NPMStoragePathCalculator( new SpecialPathManagerImpl() );
        StoreKey store = new StoreKey("npm", StoreType.hosted, "test");

        String tgzPath = "jquery/-/jquery-1.2.3-redhat-1.tgz";
        String storagePath = pathCalculator.calculateStoragePath(store, tgzPath );
        System.out.println(storagePath);
        assertTrue(tgzPath.equals(storagePath));

        String rangedTgzPath = "@discoveryjs/json-ext/-/json-ext-0.5.6.tgz";
        storagePath = pathCalculator.calculateStoragePath(store, rangedTgzPath );
        System.out.println(storagePath);
        assertTrue(rangedTgzPath.equals(storagePath));

        String packagePath = "jquery/package.json";
        storagePath = pathCalculator.calculateStoragePath(store, packagePath );
        System.out.println(storagePath);
        assertTrue(packagePath.equals(storagePath));

        String packagePath2 = "jquery";
        storagePath = pathCalculator.calculateStoragePath(store, packagePath2 );
        System.out.println(storagePath);
        assertTrue(packagePath.equals(storagePath));

        String packagePathHttpMeta = "jquery/package.json.http-metadata.json";
        storagePath = pathCalculator.calculateStoragePath(store, packagePathHttpMeta );
        System.out.println(storagePath);
        assertTrue(packagePathHttpMeta.equals(storagePath));

        String rangedPackagePath = "@discoveryjs/json-ext/package.json";
        storagePath = pathCalculator.calculateStoragePath(store, rangedPackagePath );
        System.out.println(storagePath);
        assertTrue(rangedPackagePath.equals(storagePath));

        String rangedPackagePath2 = "@discoveryjs/json-ext";
        storagePath = pathCalculator.calculateStoragePath(store, rangedPackagePath2 );
        System.out.println(storagePath);
        assertTrue(rangedPackagePath.equals(storagePath));

        String rangedPackagePathHttpMeta = "@discoveryjs/json-ext/package.json.http-metadata.json";
        storagePath = pathCalculator.calculateStoragePath(store, rangedPackagePathHttpMeta );
        System.out.println(storagePath);
        assertTrue(rangedPackagePathHttpMeta.equals(storagePath));
    }

}
