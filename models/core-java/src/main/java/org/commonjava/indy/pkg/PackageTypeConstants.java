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
package org.commonjava.indy.pkg;

/**
 * Created by ruhan on 7/24/18.
 */
public class PackageTypeConstants
{
    public static final String PKG_TYPE_MAVEN = "maven";

    public static final String PKG_TYPE_NPM = "npm";

    public static final String PKG_TYPE_GENERIC_HTTP = "generic-http";

    public static boolean isValidPackageType( final String pkgType )
    {
        return PKG_TYPE_MAVEN.equals( pkgType ) || PKG_TYPE_NPM.equals( pkgType ) || PKG_TYPE_GENERIC_HTTP.equals(
                pkgType );
    }
}
