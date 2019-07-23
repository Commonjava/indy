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
package org.commonjava.indy.folo.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by jdcasey on 9/9/15.
 */
public final class FoloFileTypes
{

    public static final String RECORD_JSON = "json";

    public static final String REPO_ZIP = "repo.zip";

    public static final Set<String> TYPES =
            Collections.unmodifiableSet( new HashSet<>( Arrays.asList( RECORD_JSON, REPO_ZIP ) ) );

    private FoloFileTypes()
    {
    }
}
