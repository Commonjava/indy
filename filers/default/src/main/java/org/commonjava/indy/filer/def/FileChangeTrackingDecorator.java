package org.commonjava.indy.filer.def;

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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.slf4j.LoggerFactory.getLogger;

public class FileChangeTrackingDecorator
                extends AbstractTransferDecorator
{
    private final Logger logger = getLogger(getClass().getName());

    private final DefaultStorageProviderConfiguration config;

    private volatile int changeCounter;

    private File currentFile;

    private long nextRollDate;

    FileChangeTrackingDecorator( DefaultStorageProviderConfiguration config )
    {
        this.config = config;
        resetTrackingFile();
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
//        logger.info( "Logging copy-from to changed-file: {}", transfer.getPath() );
        writeChangedPath( transfer );
    }

    @Override
    public void decorateDelete( Transfer transfer, EventMetadata metadata ) throws IOException
    {
        super.decorateDelete( transfer, metadata );
//        logger.info( "Logging delete to changed-file: {}", transfer.getPath() );
        writeChangedPath( transfer );
    }

    @Override
    public void decorateCreateFile( Transfer transfer, EventMetadata metadata ) throws IOException
    {
        super.decorateCreateFile( transfer, metadata );
//        logger.info( "Logging create-file to changed-file: {}", transfer.getPath() );
        writeChangedPath( transfer );
    }

    private final synchronized void writeChangedPath( Transfer transfer )
    {
        Path storageRoot = config.getStorageRootDirectory().toPath().toAbsolutePath().normalize();
        Path transferPath = transfer.getDetachedFile().toPath().toAbsolutePath().normalize();
        if ( transferPath.startsWith( storageRoot ) )
        {
            try ( FileWriter fw = new FileWriter( getCurrentListFile(), true ) )
            {
//                logger.info( "Change counter: {}", changeCounter );
                if ( changeCounter > 1 )
                {
                    fw.write( "\n" );
                }

                fw.write( "./" + storageRoot.relativize( transferPath ) );
                changeCounter++;
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
            resetTrackingFile();
        }
        else
        {
            if ( System.currentTimeMillis() >= this.nextRollDate )
            {
                resetTrackingFile();
            }
        }

        return currentFile;
    }

    private void resetTrackingFile()
    {
        Date currentDate = new Date();
        Calendar c = Calendar.getInstance();
        c.setTime( currentDate );
        c.set(Calendar.DATE, c.get( Calendar.DATE ) + 1);
        c.set( Calendar.HOUR, 0 );
        c.set( Calendar.MINUTE, 0 );
        c.set( Calendar.SECOND, 0 );
        c.set( Calendar.MILLISECOND, 0 );
        this.nextRollDate = c.getTime().getTime();
        this.currentFile = Paths.get( config.getChangeTrackingDirectory() )
                                .resolve( new SimpleDateFormat( "yyyy-MM-dd'/'hh'.'mm'.'ss'.'SSS'.lst'" ).format( currentDate ) )
                                .toFile();

        this.currentFile.getParentFile().mkdirs();
        this.changeCounter = 0;
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
//            logger.info( "Logging write to changed-file: {}", transfer.getPath() );
            writeChangedPath( transfer );
        }
    }
}
