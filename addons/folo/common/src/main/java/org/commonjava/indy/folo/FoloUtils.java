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
package org.commonjava.indy.folo;

import org.commonjava.indy.folo.model.TrackedContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.apache.commons.io.IOUtils.copy;
import static org.commonjava.indy.folo.ctl.FoloConstants.TRACKING_TYPE.SEALED;

public class FoloUtils
{
    private static final Logger logger = LoggerFactory.getLogger( FoloUtils.class );

    /**
     * Write sealed records to a zip file.
     */
    public static void zipTrackedContent( File out, Set<TrackedContent> sealed ) throws IOException
    {
        logger.info( "Writing sealed zip to: '{}'", out.getAbsolutePath() );

        try (ZipOutputStream zip = new ZipOutputStream( new FileOutputStream( out ) ))
        {
            for ( TrackedContent f : sealed )
            {
                String name = SEALED.getValue() + "/" + f.getKey().getId();

                logger.trace( "Adding {} to zip", name );
                zip.putNextEntry( new ZipEntry( name ) );
                copy( toInputStream( f ), zip );
            }
        }
    }

    public static void backupTrackedContent( File dir, Set<TrackedContent> sealed ) throws IOException
    {
        logger.info( "Backup sealed to: '{}', size: {}", dir.getAbsolutePath(), sealed.size() );
        for ( TrackedContent f : sealed )
        {
            String name = f.getKey().getId();
            logger.trace( "Adding {} to {}", name, dir );
            copy( toInputStream( f ), new FileOutputStream( new File( dir, name ) ) );
        }
    }

    /**
     * Read records from input stream and execute consumer function.
     * @param inputStream
     * @param consumer
     * @return count of records read from the stream
     */
    public static int readZipInputStreamAnd( InputStream inputStream, Consumer<TrackedContent> consumer )
                    throws IOException, ClassNotFoundException
    {
        int count = 0;
        try ( ZipInputStream stream = new ZipInputStream( inputStream ) )
        {
            ZipEntry entry;
            while((entry = stream.getNextEntry())!=null)
            {
                logger.trace( "Read entry: %s, len: %d", entry.getName(), entry.getSize() );
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int len;
                byte[] buffer = new byte[1024];
                while ((len = stream.read( buffer)) > 0)
                {
                    bos.write(buffer, 0, len);
                }
                bos.close();

                ObjectInputStream ois = new ObjectInputStream( new ByteArrayInputStream( bos.toByteArray() ));
                TrackedContent record = (TrackedContent) ois.readObject();
                consumer.accept( record );
                count++;
            }
        }
        return count;
    }

    public static InputStream toInputStream( TrackedContent f ) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( f );
        oos.flush();
        oos.close();

        return new ByteArrayInputStream( baos.toByteArray() );
    }

}
