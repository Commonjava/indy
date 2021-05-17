package org.commonjava.indy.filer.def;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.AbstractTransferDecorator;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import static org.slf4j.LoggerFactory.getLogger;

public class FileChangeTrackingDecorator
                extends AbstractTransferDecorator
{
    private final Logger logger = getLogger(getClass().getName());

    private final DefaultStorageProviderConfiguration config;

    private volatile int changeCounter = 0;

    private File currentFile;

    FileChangeTrackingDecorator( DefaultStorageProviderConfiguration config )
    {
        this.config = config;
        this.currentFile = generateCurrentFile();
    }

    private File generateCurrentFile()
    {
        File listFile = Path.of( config.getChangeTrackingDirectory() )
                     .resolve( new SimpleDateFormat( "yyyy-MM-ddThhmmss.lst" ).format( new Date() ) )
                     .toFile();

        return listFile;
    }

    @Override
    public OutputStream decorateWrite( OutputStream stream, Transfer transfer, TransferOperation op,
                                       EventMetadata metadata ) throws IOException
    {
        return new ChangeTrackingOutputStream( super.decorateWrite( stream, transfer, op, metadata ), transfer );
    }

    @Override
    public void decorateCopyFrom( Transfer from, Transfer transfer, EventMetadata metadata ) throws IOException
    {
        super.decorateCopyFrom( from, transfer, metadata );
        writeChangedPath( transfer );
    }

    @Override
    public void decorateDelete( Transfer transfer, EventMetadata metadata ) throws IOException
    {
        super.decorateDelete( transfer, metadata );
        writeChangedPath( transfer );
    }

    @Override
    public void decorateCreateFile( Transfer transfer, EventMetadata metadata ) throws IOException
    {
        super.decorateCreateFile( transfer, metadata );
        writeChangedPath( transfer );
    }

    private final synchronized void writeChangedPath( Transfer transfer )
    {
        Path storageRoot = config.getStorageRootDirectory().toPath().toAbsolutePath().normalize();
        Path transferPath = transfer.getDetachedFile().toPath().toAbsolutePath().normalize();
        if ( transferPath.startsWith( storageRoot ) )
        {
            try ( FileWriter fw = new FileWriter( getCurrentListFile() ) )
            {
                if ( changeCounter < 1 )
                {
                    fw.write( "\n" );
                }
                fw.write( "./" + transferPath.relativize( storageRoot ) );
            }
            catch ( IOException | IllegalArgumentException e )
            {
                logger.error( "Failed to write file change: " + transfer.getPath() + " to log: " + currentFile, e );
            }
        }
    }

    private synchronized File getCurrentListFile()
    {
        if ( changeCounter >= config.getChangeTrackingRollSize() )
        {
            this.currentFile = generateCurrentFile();
            this.changeCounter = 0;
        }

        return currentFile;
    }

    private class ChangeTrackingOutputStream
                    extends FilterOutputStream
    {
        private Transfer transfer;

        public ChangeTrackingOutputStream( OutputStream outputStream, Transfer transfer )
        {
            super( outputStream );
            this.transfer = transfer;
        }

        @Override
        public void close() throws IOException
        {
            super.close();
            writeChangedPath( transfer );
        }
    }
}
