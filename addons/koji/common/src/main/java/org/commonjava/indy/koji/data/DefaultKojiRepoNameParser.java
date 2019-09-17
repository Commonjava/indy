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
package org.commonjava.indy.koji.data;

/**
 * Created by ruhan on 3/26/18.
 */
public class DefaultKojiRepoNameParser
                implements KojiRepoNameParser
{
    private static final String KOJI_ = "koji-";

    private static final String KOJI_BINARY_ = "koji-binary-";

    /**
     * Retrieve what is behind koji- or koji-binary-
     * @param repoName
     * @return koji build nvr
     */
    public String parse( String repoName )
    {
        String prefix = null;
        if ( repoName.startsWith( KOJI_BINARY_ ) )
        {
            prefix = KOJI_BINARY_;
        }
        else if ( repoName.startsWith( KOJI_ ) )
        {
            prefix = KOJI_;
        }

        if ( prefix != null )
        {
            return repoName.substring( prefix.length() );
        }
        return null;
    }
}
