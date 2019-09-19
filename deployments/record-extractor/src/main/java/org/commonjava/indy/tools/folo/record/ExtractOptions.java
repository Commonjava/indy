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

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;

public class ExtractOptions
{
    @Option( name = "-h", aliases = { "--help" }, usage = "Print this and exit" )
    private boolean help;

    @Argument( index = 0, metaVar = "data-file", required = false, usage = "Record file to read")
    private File dataFile;

    @Argument( index = 1, metaVar = "out-file", required = false, usage = "JSON file to write")
    private File outFile;

    public boolean isHelp()
    {
        return help;
    }

    public void setHelp( final boolean help )
    {
        this.help = help;
    }

    public File getDataFile()
    {
        return dataFile;
    }

    public void setDataFile( final File dataFile )
    {
        this.dataFile = dataFile;
    }

    public File getOutFile()
    {
        return outFile;
    }

    public void setOutFile( final File outFile )
    {
        this.outFile = outFile;
    }

    public boolean parseArgs( final String[] args )
            throws CmdLineException
    {
        final CmdLineParser parser = new CmdLineParser( this );
        boolean canStart = true;
        parser.parseArgument( args );

        if ( isHelp() )
        {
            printUsage( parser, null );
            canStart = false;
        }
        else if ( getDataFile() == null )
        {
            System.err.println("You must provide 'action', 'cache-name', and 'data-file' arguments!");
            printUsage( parser, null );

            canStart = false;
        }

        return canStart;
    }

    public static void printUsage( final CmdLineParser parser, final CmdLineException error )
    {
        if ( error != null )
        {
            System.err.println( "Invalid option(s): " + error.getMessage() );
            System.err.println();
        }

        System.err.println( "Usage: $0 [OPTIONS] [<target-path>]" );
        System.err.println();
        System.err.println();
        // If we are running under a Linux shell COLUMNS might be available for the width
        // of the terminal.
        parser.setUsageWidth( ( System.getenv( "COLUMNS" ) == null ? 100 : Integer.valueOf( System.getenv( "COLUMNS" ) ) ) );
        parser.printUsage( System.err );
        System.err.println();
    }
}
