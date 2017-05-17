package org.commonjava.indy.metrics.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xiabai on 5/9/17.
 */
public class ZabbixSocketServer
                implements Runnable
{
    private ConcurrentHashMap<String, Expectation> expections = new ConcurrentHashMap<String, Expectation>();

    public boolean isStartFlag()
    {
        return startFlag;
    }

    public void setStartFlag( boolean startFlag )
    {
        this.startFlag = startFlag;
    }

    private boolean startFlag = true;

    public Expectation getExpection( String method )
    {
        return expections.get( method );
    }

    public void setExpection( String method, Expectation expectation )
    {
        expections.put( method, expectation );
    }

    public void run()
    {
        ServerSocket listener = null;
        try
        {
            int clientNumber = 0;
            listener = new ServerSocket( 9999 );

            while ( startFlag )
            {

                new Thread( new Capitalizer( listener.accept(), clientNumber++, expections ) ).start();
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
        finally
        {
            try
            {
                listener.close();
            }
            catch ( IOException e )
            {
                e.printStackTrace();
            }
        }
    }

}