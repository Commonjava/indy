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
package org.commonjava.indy.metrics.socket;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;

import static org.commonjava.test.http.util.PortFinder.findPortFor;

/**
 * Created by xiabai on 5/9/17.
 */
public class ZabbixSocketServer
                implements Runnable
{
    private ConcurrentHashMap<String, Expectation> expections = new ConcurrentHashMap<String, Expectation>();

    private int port;

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

    public int getPort()
    {
        return port;
    }

    public void run()
    {
        ServerSocket listener = null;
        try
        {
            int clientNumber = 0;
            listener = new ServerSocket( findPortFor( 16, p -> p ) );

            this.port = listener.getLocalPort();

            synchronized ( this )
            {
                notifyAll();
            }

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