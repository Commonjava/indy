package org.commonjava.indy.ftest.core.fixture;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ExceptionInputStream
                extends ByteArrayInputStream
{

    public ExceptionInputStream( byte[] buf )
    {
        super( buf );
    }

    int index = 0;

    @Override
    public int read()
    {
        if ( this.pos < buf.length )
        {
            return super.read();

        }

        return -1;
    }

    @Override
    public int read( byte[] buf ) throws IOException
    {
        if ( index < 2 )
        {
            int result = -1;
            result = super.read( buf );
            index++;

            return result;
        }
        return -1;
    }

    public static void copy( ExceptionInputStream in, OutputStream out) throws IOException
    {
        byte[] buffer = new byte[64];
        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1)
                break;
            out.write(buffer, 0, bytesRead);
        }
    }
}