/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.pathmap.migrate;

import org.apache.commons.lang3.StringUtils;
import org.commonjava.storage.pathmapped.util.ChecksumCalculator;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.commonjava.indy.pathmap.migrate.Util.CMD_MIGRATE;
import static org.commonjava.indy.pathmap.migrate.Util.CMD_SCAN;
import static org.commonjava.indy.pathmap.migrate.Util.DEFAULT_BASE_DIR;
import static org.commonjava.indy.pathmap.migrate.Util.DEFAULT_BATCH_SIZE;
import static org.commonjava.indy.pathmap.migrate.Util.DEFAULT_WORK_DIR;
import static org.commonjava.indy.pathmap.migrate.Util.PROCESSED_FILES_DIR;
import static org.commonjava.indy.pathmap.migrate.Util.STATUS_FILE;
import static org.commonjava.indy.pathmap.migrate.Util.TODO_FILES_DIR;
import static org.commonjava.storage.pathmapped.pathdb.datastax.util.CassandraPathDBUtils.PROP_CASSANDRA_HOST;
import static org.commonjava.storage.pathmapped.pathdb.datastax.util.CassandraPathDBUtils.PROP_CASSANDRA_KEYSPACE;
import static org.commonjava.storage.pathmapped.pathdb.datastax.util.CassandraPathDBUtils.PROP_CASSANDRA_PASS;
import static org.commonjava.storage.pathmapped.pathdb.datastax.util.CassandraPathDBUtils.PROP_CASSANDRA_PORT;
import static org.commonjava.storage.pathmapped.pathdb.datastax.util.CassandraPathDBUtils.PROP_CASSANDRA_USER;

public class MigrateOptions
{
    @Option( name = "-h", aliases = "--help", usage = "Print this and exit" )
    private boolean help;

    @Option( name = "-b", aliases = "--base", usage = "Base dir of storage for all indy artifacts" )
    private String baseDir;

    @Option( name = "-w", aliases = "--workdir", usage = "Work dir to store all generated working files" )
    private String workDir;

    @Option( name = "-f", aliases = "--filter",
             usage = "Regex style filter string to filter some files which are unwanted" )
    private String filterPattern;

    //    @Option( name = "-t", aliases = "--threads", usage = "Number of threads to execute the migrating process" )
    //    private int threads;

    @Option( name = "-B", aliases = "--batch", usage = "Batch of paths to process each time" )
    private int batchSize;

    @Option( name = "-d", aliases = "--dedupe", usage = "If to use checksum to dedupe all files in file storage" )
    private boolean dedupe;

    @Option( name = "-A", aliases = "--dedupeAlgorithm", usage = "Algorithm to do dedupe check, default is MD5" )
    private String dedupeAlgorithm;

    @Option( name = "-H", aliases = "--host", usage = "Cassandra server hostname" )
    private String cassandraHost;

    @Option( name = "-P", aliases = "--port", usage = "Cassandra server port" )
    private String cassandraPort;

    @Option( name = "-u", aliases = "--user", usage = "Cassandra server username" )
    private String cassandraUser;

    @Option( name = "-p", aliases = "--password", usage = "Cassandra server password" )
    private String cassandraPass;

    @Option( name = "-k", aliases = "--keyspace", usage = "Cassandra server keyspace" )
    private String cassandraKeyspace;

    @Argument( index = 0, metaVar = "command", usage = "Name of command to run, use scan | migrate | resume" )
    private String command;

    public boolean isHelp()
    {
        return help;
    }

    public void setHelp( boolean help )
    {
        this.help = help;
    }

    public String getBaseDir()
    {
        return isBlank( baseDir ) ? DEFAULT_BASE_DIR : baseDir;
    }

    public void setBaseDir( String baseDir )
    {
        this.baseDir = baseDir;
    }

    public String getWorkDir()
    {
        return isBlank( workDir ) ? DEFAULT_WORK_DIR : workDir;
    }

    public void setWorkDir( String workDir )
    {
        this.workDir = workDir;
    }

    public String getFilterPattern()
    {
        return filterPattern;
    }

    public void setFilterPattern( String filterPattern )
    {
        this.filterPattern = filterPattern;
    }

    //    public int getThreads()
    //    {
    //        return threads <= 0 ? DEFAULT_THREADS_NUM : threads;
    //    }
    //
    //    public void setThreads( int threads )
    //    {
    //        this.threads = threads;
    //    }

    public int getBatchSize()
    {
        return batchSize <= 0 ? DEFAULT_BATCH_SIZE : batchSize;
    }

    public void setBatchSize( int batchSize )
    {
        this.batchSize = batchSize;
    }

    public boolean isDedupe()
    {
        return dedupe;
    }

    public void setDedupe( boolean dedupe )
    {
        this.dedupe = dedupe;
    }

    public String getDedupeAlgorithm()
    {
        return StringUtils.isBlank( dedupeAlgorithm ) ? "MD5" : dedupeAlgorithm;
    }

    public void setDedupeAlgorithm( String dedupeAlgorithm )
    {
        this.dedupeAlgorithm = dedupeAlgorithm;
    }

    public void setMigrator( CassandraMigrator migrator )
    {
        this.migrator = migrator;
    }

    public String getCommand()
    {
        return command == null ? "" : command.toLowerCase().trim();
    }

    public void setCommand( String command )
    {
        this.command = command;
    }

    public String getCassandraHost()
    {
        return StringUtils.isBlank( cassandraHost ) ? "localhost" : cassandraHost;
    }

    public void setCassandraHost( String cassandraHost )
    {
        this.cassandraHost = cassandraHost;
    }

    public String getCassandraPort()
    {
        return StringUtils.isBlank( cassandraPort ) ? "9142" : cassandraPort;
    }

    public void setCassandraPort( String cassandraPort )
    {
        this.cassandraPort = cassandraPort;
    }

    public String getCassandraUser()
    {
        return cassandraUser;
    }

    public void setCassandraUser( String cassandraUser )
    {
        this.cassandraUser = cassandraUser;
    }

    public String getCassandraPass()
    {
        return cassandraPass;
    }

    public void setCassandraPass( String cassandraPass )
    {
        this.cassandraPass = cassandraPass;
    }

    public String getCassandraKeyspace()
    {
        return cassandraKeyspace;
    }

    public void setCassandraKeyspace( String cassandraKeyspace )
    {
        this.cassandraKeyspace = cassandraKeyspace;
    }

    public boolean parseArgs( final String[] args )
    {
        final CmdLineParser parser = new CmdLineParser( this );
        try
        {
            parser.parseArgument( args );
        }
        catch ( final CmdLineException e )
        {
            e.printStackTrace();
            throw new IllegalArgumentException(
                    String.format( "Failed to parse command-line args: %s", Arrays.toString( args ) ), e );

        }

        if ( isHelp() )
        {
            printUsage( parser, null );
        }

        if ( StringUtils.isBlank( getCommand() ) )
        {
            System.out.println( "Command can not be null" );
            return false;
        }
        final String cmd = getCommand().toLowerCase().trim();
        if ( !cmd.equals( Util.CMD_SCAN ) && !cmd.equals( Util.CMD_MIGRATE ) )
        {
            System.out.println( String.format( "Invalid command %s, use scan | migrate | resume", cmd ) );
            return false;
        }

        return validateOptions();

    }

    private boolean validateOptions()
    {
        System.out.println( String.format( "Working dir for whole migration process: %s", getAbsoluteWorkDir() ) );
        if ( getCommand().equals( CMD_SCAN ) )
        {
            System.out.println( String.format( "Base storage dir for artifacts: %s", getBaseDir() ) );
            System.out.println( String.format( "Batch of paths to process each time: %s", getBatchSize() ) );
            System.out.println( String.format( "Filter pattern for unwanted files: %s", getFilterPattern() ) );
        }

        if ( getCommand().equals( CMD_MIGRATE ) )
        {
            System.out.println( String.format( "Cassandra server host: %s", getCassandraHost() ) );
            System.out.println( String.format( "Cassandra server port: %s", getCassandraPort() ) );
            System.out.println( String.format( "Cassandra server username: %s", getCassandraUser() ) );
            System.out.println( String.format( "Cassandra server keyspace: %s", getCassandraKeyspace() ) );
            System.out.println( String.format( "Will use checksum to dedupe files? %s", isDedupe() ) );
        }

        System.out.println();
        //        System.out.println( String.format( "Threads number to run the whole process? %s", getThreads() ) );
        if ( getCommand().equals( CMD_SCAN ) && !validateBaseDir() )
        {
            return false;
        }

        if ( getCommand().equals( CMD_MIGRATE ) )
        {
            return validateTodoDir() && validateCassandra();
        }

        return true;
    }

    private boolean validateBaseDir()
    {
        Path basePath = Paths.get( getBaseDir() );
        if ( !Files.isDirectory( basePath ) )
        {
            System.out.println( String.format( "Error: base dir %s is not a directory", getBaseDir() ) );
            return false;
        }
        List<String> childs = new ArrayList<>( 3 );
        try
        {
            Files.walk( basePath, 1 ).map( Path::toString ).forEach( childs::add );
        }
        catch ( IOException e )
        {
            System.out.println(
                    String.format( "Error: io error happened during listing sub dirs: %s", e.getMessage() ) );
            return false;
        }
        boolean containsMaven = false;
        for ( String child : childs )
        {
            if ( child.contains( "maven" ) )
            {
                containsMaven = true;
                break;
            }
        }

        if ( !containsMaven )
        {
            System.out.println( String.format( "Error: the base dir %s is not a valid volume to store indy artifacts.",
                                               getBaseDir() ) );
            return false;
        }

        return true;
    }

    private boolean validateTodoDir()
    {
        Path todoPath = Paths.get( getToDoDir() );
        if ( !Files.isDirectory( todoPath ) )
        {
            System.out.println( String.format(
                    "Validation failed: todo folder %s in workdir %s does not exist or is not a directory. Make sure you have used 'scan' command to generate the path files in this folder.",
                    getToDoDir(), getWorkDir() ) );
            return false;
        }

        final AtomicInteger todoFilesCount = new AtomicInteger( 0 );
        try
        {
            Files.walk( Paths.get( getToDoDir() ), 1 )
                 .filter( MigrateCmd.WORKING_FILES_FILTER )
                 .forEach( p -> todoFilesCount.getAndIncrement() );
        }
        catch ( IOException e )
        {
            System.out.println( String.format( "Error: Can not list dir %s", getToDoDir() ) );
            return false;
        }

        if ( todoFilesCount.get() <= 0 )
        {
            System.out.println(
                    "Error: There are no path entries generated for migrating, please use 'scan' command first to generate them." );
            return false;
        }

        return true;
    }

    private CassandraMigrator migrator;

    public boolean validateCassandra()
    {
        try
        {
            initMigrator();
        }
        catch ( Throwable t )
        {
            System.out.println( String.format( "Error: cassandra validation failed: %s", t.getMessage() ) );
            return false;
        }
        return true;
    }

    public CassandraMigrator getMigrator()
            throws MigrateException
    {
        if ( migrator == null )
        {
            initMigrator();
        }
        return migrator;
    }

    private void initMigrator()
            throws MigrateException
    {
        if ( migrator == null )
        {
            HashMap<String, Object> cassandraProps = new HashMap<>();
            cassandraProps.put( PROP_CASSANDRA_HOST, getCassandraHost() );
            cassandraProps.put( PROP_CASSANDRA_PORT, Integer.parseInt( getCassandraPort() ) );
            cassandraProps.put( PROP_CASSANDRA_KEYSPACE, getCassandraKeyspace() );
            if ( StringUtils.isNotBlank( getCassandraUser() ) )
            {
                cassandraProps.put( PROP_CASSANDRA_USER, getCassandraUser() );
            }
            if ( StringUtils.isNotBlank( getCassandraPass() ) )
            {
                cassandraProps.put( PROP_CASSANDRA_PASS, getCassandraPass() );
            }

            if ( isDedupe() )
            {
                try
                {
                    new ChecksumCalculator( getDedupeAlgorithm() ); // verify algo
                }
                catch ( NoSuchAlgorithmException e )
                {
                    throw new MigrateException(
                            String.format( "Error: checksum algorithm not supported: %s", getDedupeAlgorithm() ), e );
                }
            }
            migrator = CassandraMigrator.getMigrator( cassandraProps, getBaseDir(), isDedupe(), getDedupeAlgorithm() );
        }
    }

    public static void printUsage( final CmdLineParser parser, final CmdLineException error )
    {
        if ( error != null )
        {
            System.out.println( "Invalid option(s): " + error.getMessage() );
            System.out.println();
        }

        System.out.println( "Usage: $0 $command [OPTIONS]" );
        System.out.println();
        System.out.println();
        // If we are running under a Linux shell COLUMNS might be available for the width
        // of the terminal.
        parser.setUsageWidth(
                ( System.getenv( "COLUMNS" ) == null ? 100 : Integer.parseInt( System.getenv( "COLUMNS" ) ) ) );
        parser.printUsage( System.out );
        System.out.println();
    }

    public String getAbsoluteWorkDir()
    {
        return Paths.get( getWorkDir() ).toAbsolutePath().toString();
    }

    public String getToDoDir()
    {
        return Paths.get( getWorkDir(), TODO_FILES_DIR ).toAbsolutePath().toString();
    }

    public String getProcessedDir()
    {
        return Paths.get( getWorkDir(), PROCESSED_FILES_DIR ).toAbsolutePath().toString();
    }

    public File getStatusFile()
            throws IOException
    {
        File statusFile = Paths.get( getWorkDir(), STATUS_FILE ).toFile();
        if ( !statusFile.exists() )
        {
            statusFile.createNewFile();
        }
        return statusFile;
    }
}
