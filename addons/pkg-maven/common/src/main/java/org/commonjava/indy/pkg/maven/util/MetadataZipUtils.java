package org.commonjava.indy.pkg.maven.util;

import org.apache.commons.lang3.SerializationUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.zip.DataFormatException;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

public class MetadataZipUtils
{
    public static byte[] zip( Serializable obj ) throws IOException
    {
        byte[] dataToCompress = SerializationUtils.serialize( obj );

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
                        GZIPOutputStream zipStream = new GZIPOutputStream( out ))
        {
            zipStream.write( dataToCompress );
            return out.toByteArray();
        }
    }

    public static <T> T unzip( byte[] compressedData ) throws IOException, DataFormatException
    {
        Inflater decompressor = new Inflater();
        decompressor.setInput( compressedData );
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while ( !decompressor.finished() )
        {
            int count = decompressor.inflate( buf );
            bos.write( buf, 0, count );
        }
        bos.close();
        byte[] decompressedData = bos.toByteArray();

        T obj = SerializationUtils.deserialize( decompressedData );
        return obj;
    }

}
