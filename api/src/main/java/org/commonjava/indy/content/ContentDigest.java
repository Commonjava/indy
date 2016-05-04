/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.indy.content;

/**
 * Types of checksums commonly used for Maven artifacts and metadata, which may be generated via a {@link ContentGenerator} when, say, metadata files
 * are merged in a group.
 */
public enum ContentDigest
{

    MD5, SHA_256( "SHA-256" ),
    SHA_1 ( "SHA-1" );

    private String digestName;

    ContentDigest()
    {
        this.digestName = name();
    }

    ContentDigest( final String digestName )
    {
        this.digestName = digestName;
    }

    public String digestName()
    {
        return digestName;
    }

}
