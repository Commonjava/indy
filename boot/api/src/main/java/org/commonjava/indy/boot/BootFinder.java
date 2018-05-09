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
package org.commonjava.indy.boot;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class BootFinder
{

    public static BootInterface find()
        throws IndyBootException
    {
        return find( Thread.currentThread()
                           .getContextClassLoader() );
    }

    public static BootInterface find( final ClassLoader classloader )
        throws IndyBootException
    {
        final InputStream stream =
            classloader.getResourceAsStream( "META-INF/services/" + BootInterface.class.getName() );
        if ( stream == null )
        {
            throw new IndyBootException( "No BootInterface implementations registered." );
        }

        List<String> lines;
        try
        {
            lines = IOUtils.readLines( stream );
        }
        catch ( final IOException e )
        {
            throw new IndyBootException( "Failed to read registration of BootInterface: " + e.getMessage(), e );
        }

        final String className = lines.get( 0 );
        try
        {
            final Class<?> cls = classloader.loadClass( className );
            return (BootInterface) cls.newInstance();
        }
        catch ( ClassNotFoundException | InstantiationException | IllegalAccessException e )
        {
            throw new IndyBootException( "Failed to initialize BootInterface: %s. Reason: %s", e, className,
                                          e.getMessage() );
        }
    }

}
