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


import static org.commonjava.indy.pkg.PackageTypeConstants.*;

/**
 * Enumeration to distinguish between different access channels to stores.
 *
 * @author pkocandr
 */
public enum AccessChannel
{

    /** Used when the store is accessed via httprox addon. */
    GENERIC_PROXY,

    /** Used to signify content coming from normal repositories and groups. */
    NATIVE,

    /** Used when the store is accessed via regular Maven repo.
     *  NOTE: This has been changed to {@link #NATIVE} in our tracking code. It is included for historical purposes. */
    @Deprecated
    MAVEN_REPO;

}
