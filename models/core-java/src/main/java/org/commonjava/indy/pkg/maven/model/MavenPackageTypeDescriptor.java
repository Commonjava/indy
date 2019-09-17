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
package org.commonjava.indy.pkg.maven.model;

import org.commonjava.indy.model.core.PackageTypeDescriptor;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;

/**
 * {@link PackageTypeDescriptor} implementation for Maven content.
 *
 * Created by jdcasey on 5/10/17.
 */
// FIXME: Move to indy-pkg-maven-model-java module as soon as we can stop defaulting package type in StoreKey to 'maven'
public class MavenPackageTypeDescriptor
        implements PackageTypeDescriptor
{
    public static final String MAVEN_PKG_KEY = PKG_TYPE_MAVEN;

    public static final String MAVEN_CONTENT_REST_BASE_PATH = "/api/content/maven";

    public static final String MAVEN_ADMIN_REST_BASE_PATH = "/api/admin/stores/maven";

    @Override
    public String getKey()
    {
        return MAVEN_PKG_KEY;
    }

    @Override
    public String getContentRestBasePath()
    {
        return MAVEN_CONTENT_REST_BASE_PATH;
    }

    @Override
    public String getAdminRestBasePath()
    {
        return MAVEN_ADMIN_REST_BASE_PATH;
    }
}
