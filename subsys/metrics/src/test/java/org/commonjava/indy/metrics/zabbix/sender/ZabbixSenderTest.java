package org.commonjava.indy.metrics.zabbix.sender;

import org.commonjava.indy.metrics.socket.Expectation;
import org.commonjava.indy.metrics.socket.ZabbixSocketServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.fail;

/**
 * Created by xiabai on 5/9/17.
 */
public class ZabbixSenderTest
{
    private ZabbixSocketServer socketServer;

    private DataObject dataObject;

    @Before
    public void socketStart() throws Exception
    {
        socketServer = new ZabbixSocketServer();
        dataObject = new DataObject();
        dataObject.setClock( 1000 );
        dataObject.setHost( "test" );
        dataObject.setKey( "test-item" );
        dataObject.setValue( "123" );
        SenderRequest sr = new SenderRequest();
        sr.setRequest( "dataSend" );
        sr.setData( new ArrayList<>( Collections.singletonList( dataObject ) ) );
        SenderResult senderResult = new SenderResult();
        senderResult.setFailed( 0 );
        senderResult.setProcessed( 1 );
        senderResult.setTotal( 1 );
        senderResult.setSpentSeconds( 1l );
        System.out.print( sr.toString() );
        Expectation expectation = new Expectation( "sender data", sr.toBytes(), senderResult );
        socketServer.setExpection( "sender data", expectation );
        Thread t = new Thread( socketServer );
        t.start();

        synchronized ( socketServer )
        {
            socketServer.wait();
        }
    }

    @After
    public void shutDown() throws Exception
    {
        socketServer.setStartFlag( false );
    }

    @Test
    public void dataSend() throws Exception
    {

        ZabbixSender sender = new ZabbixSender( "localhost", socketServer.getPort() );
        try
        {

            SenderResult result = sender.send( dataObject );
            org.junit.Assert.assertEquals( result.getProcessed(), result.getTotal() );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            fail( "Error trying to send/receive data" );
        }
    }
}
