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
package org.commonjava.indy.model.core;

import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_GENERIC_HTTP;

/**
 * Package type to handle generic http content, as is used by httprox.
 *
 * Created by jdcasey on 5/10/17.
 */
public class GenericPackageTypeDescriptor
    implements PackageTypeDescriptor
{
    public static final String GENERIC_PKG_KEY = PKG_TYPE_GENERIC_HTTP;

    public static final String GENERIC_CONTENT_REST_BASE_PATH = "/api/content/generic";

    public static final String GENERIC_ADMIN_REST_BASE_PATH = "/api/admin/stores/generic";

    @Override
    public String getKey()
    {
        return GENERIC_PKG_KEY;
    }

    @Override
    public String getContentRestBasePath()
    {
        return GENERIC_CONTENT_REST_BASE_PATH;
    }

    @Override
    public String getAdminRestBasePath()
    {
        return GENERIC_ADMIN_REST_BASE_PATH;
    }
}
