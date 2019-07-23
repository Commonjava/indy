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
package org.commonjava.indy.pkg.npm.model;

import org.commonjava.indy.model.core.PackageTypeDescriptor;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_NPM;

/**
 * {@link PackageTypeDescriptor} implementation for NPM content.
 *
 * Created by yma on 5/23/17.
 */
public class NPMPackageTypeDescriptor
                implements PackageTypeDescriptor
{
    public static final String NPM_PKG_KEY = PKG_TYPE_NPM;

    public static final String NPM_CONTENT_REST_BASE_PATH = "/api/content/npm";

    public static final String NPM_ADMIN_REST_BASE_PATH = "/api/admin/stores/npm";

    @Override
    public String getKey()
    {
        return NPM_PKG_KEY;
    }

    @Override
    public String getContentRestBasePath()
    {
        return NPM_CONTENT_REST_BASE_PATH;
    }

    @Override
    public String getAdminRestBasePath()
    {
        return NPM_ADMIN_REST_BASE_PATH;
    }
}
