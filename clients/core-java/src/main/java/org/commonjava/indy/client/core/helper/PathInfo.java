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
package org.commonjava.indy.client.core.helper;

import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import org.commonjava.indy.model.util.HttpUtils;

public class PathInfo
{
    private final boolean exists;

    private final String contentType;

    private final long contentLength;

    private final Date lastModified;

    public PathInfo( final Map<String, String> headers )
    {
        if ( headers == null )
        {
            exists = false;
            contentType = null;
            contentLength = -1;
            lastModified = null;
        }
        else
        {
            exists = true;
            
            contentType = headers.get( "content-type" );
            
            final String cl = headers.get("content-length");
            contentLength = cl == null ? -1 : Long.parseLong( cl );
            
            final String lm = headers.get( "last-modified" );

            Date lastModified;
            try
            {
                lastModified = lm == null ? null : HttpUtils.parseDateHeader( lm );
            }
            catch ( final ParseException e )
            {
                lastModified = null;
            }

            this.lastModified = lastModified;
        }
    }

    public boolean exists()
    {
        return exists;
    }

    public String getContentType()
    {
        return contentType;
    }

    public long getContentLength()
    {
        return contentLength;
    }

    public Date getLastModified()
    {
        return lastModified;
    }

    @Override
    public String toString()
    {
        return String.format( "PathInfo [exists=%s, contentType=%s, contentLength=%s, lastModified=%s]", exists,
                              contentType, contentLength, lastModified );
    }

}
