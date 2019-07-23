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
package org.commonjava.indy.util;

import org.apache.commons.io.IOUtils;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.io.InputStream;

@ApplicationScoped
public class MimeTyper
{

    private static final String EXTRA_MIME_TYPES = "extra-mime.types";

    private final MimetypesFileTypeMap typeMap = new MimetypesFileTypeMap();

    public MimeTyper()
    {
        final InputStream stream = Thread.currentThread()
                                         .getContextClassLoader()
                                         .getResourceAsStream( EXTRA_MIME_TYPES );
        if ( stream != null )
        {
            try
            {
                final String extraTypes = IOUtils.toString( stream );
                typeMap.addMimeTypes( extraTypes );
            }
            catch ( final IOException e )
            {
                LoggerFactory.getLogger( getClass() )
                             .error( "Cannot read extra mime types from classpath: " + EXTRA_MIME_TYPES, e );
            }
        }
    }

    public String getContentType( final String path )
    {
        return typeMap.getContentType( path );
    }

}
