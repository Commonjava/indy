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
package org.commonjava.indy.tools.cache;

import org.commonjava.propulsor.boot.BootException;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;

public class MigrationOptions
{

    @Option( name = "-h", aliases = { "--help" }, usage = "Print this and exit" )
    private boolean help;

    @Option( name = "-i", aliases = { "--infinispan-xml" }, usage = "Infinispan configuration XML to use during migration" )
    private File infinispanXml;

    @Option( name = "-t", aliases = { "--data-type" }, usage = "Data type ('json' or 'object')" )
    private DataType dataType;

    @Argument( index = 0, metaVar = "action", required = false, usage = "Migration command to execute ('dump' or 'load')")
    private MigrationCommand migrationCommand;

    @Argument( index = 1, metaVar = "cache-name", required = false, usage = "Name of cache to migrate")
    private String cacheName;

    @Argument( index = 2, metaVar = "data-file", required = false, usage = "Cache data file (dump to here, or load from here)")
    private File dataFile;

    public boolean isHelp()
    {
        return help;
    }

    public void setHelp( final boolean help )
    {
        this.help = help;
    }

    public File getInfinispanXml()
    {
        return infinispanXml;
    }

    public void setInfinispanXml( final File infinispanXml )
    {
        this.infinispanXml = infinispanXml;
    }

    public MigrationCommand getMigrationCommand()
    {
        return migrationCommand;
    }

    public void setMigrationCommand( final MigrationCommand migrationCommand )
    {
        this.migrationCommand = migrationCommand;
    }

    public String getCacheName()
    {
        return cacheName;
    }

    public void setCacheName( final String cacheName )
    {
        this.cacheName = cacheName;
    }

    public File getDataFile()
    {
        return dataFile;
    }

    public void setDataFile( final File dataFile )
    {
        this.dataFile = dataFile;
    }

    public DataType getDataType() { return dataType; }

    public void setDataType( final DataType dataType ) { this.dataType = dataType; }

    public boolean parseArgs( final String[] args )
            throws BootException
    {
        final CmdLineParser parser = new CmdLineParser( this );
        boolean canStart = true;
        try
        {
            parser.parseArgument( args );
        }
        catch ( final CmdLineException e )
        {
            throw new BootException( "Failed to parse command-line args: %s", e, e.getMessage() );
        }

        if ( isHelp() )
        {
            printUsage( parser, null );
            canStart = false;
        }
        else if ( getMigrationCommand() == null || getDataFile() == null || getCacheName() == null )
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
