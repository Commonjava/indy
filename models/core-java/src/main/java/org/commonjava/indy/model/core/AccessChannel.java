/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
import static org.commonjava.indy.pkg.PackageTypeConstants.PKG_TYPE_MAVEN;

/**
 * Enumeration to distinguish between different access channels to stores.
 *
 * @author pkocandr
 */
public enum AccessChannel
{

    /** Used when the store is accessed via httprox addon. */
    GENERIC_PROXY(PKG_TYPE_GENERIC_HTTP),
    /** Used when the store is accessed via regular Maven repo. */
    MAVEN_REPO(PKG_TYPE_MAVEN);

    private final String packageType;

    AccessChannel(String packageType)
    {
        this.packageType = packageType;
    }

    public String packageType()
    {
        return packageType;
    }

}
