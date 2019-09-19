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
package org.commonjava.indy.tools.folo.record;

import org.commonjava.indy.folo.model.TrackedContent;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.kohsuke.args4j.CmdLineException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;

public class Main
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final int ERR_INIT = 1;
    private static final int ERR_PARSE_ARGS = 2;

    public static void main( String[] args )
    {
        Thread.currentThread()
              .setUncaughtExceptionHandler( ( thread, error ) -> {
                  if ( error instanceof InvocationTargetException )
                  {
                      final InvocationTargetException ite = (InvocationTargetException) error;
                      System.err.println( "In: " + thread.getName() + "(" + thread.getId()
                                                  + "), caught InvocationTargetException:" );
                      ite.getTargetException()
                         .printStackTrace();

                      System.err.println( "...via:" );
                      error.printStackTrace();
                  }
                  else
                  {
                      System.err.println( "In: " + thread.getName() + "(" + thread.getId() + ") Uncaught error:" );
                      error.printStackTrace();
                  }
              } );

        ExtractOptions options = new ExtractOptions();
        try
        {
            if ( options.parseArgs( args ) )
            {
                try
                {
                    int result = new Main().run( options );
                    if ( result != 0 )
                    {
                        System.exit( result );
                    }
                }
                catch ( final Exception e )
                {
                    System.err.printf( "ERROR INITIALIZING BOOTER: %s", e.getMessage() );
                    System.exit( ERR_INIT );
                }
            }
        }
        catch ( final CmdLineException e )
        {
            System.err.printf( "ERROR: %s", e.getMessage() );
            System.exit( ERR_PARSE_ARGS );
        }
    }

    private int run( final ExtractOptions options )
            throws Exception
    {
        try
        {
            File dataFile = options.getDataFile();
            File outFile = options.getOutFile();
            try (ObjectInputStream ion = new ObjectInputStream( new FileInputStream( dataFile ) );
                 FileOutputStream fos = new FileOutputStream( outFile ) )
            {
                TrackedContent content = (TrackedContent) ion.readObject();
                new IndyObjectMapper( true ).writeValue( fos, content );
            }
        }
        catch ( final Throwable e )
        {
            if ( e instanceof Exception )
                throw (Exception)e;

            logger.error( "Failed to initialize Booter: " + e.getMessage(), e );
            return ERR_INIT;
        }

        return 0;
    }
}
