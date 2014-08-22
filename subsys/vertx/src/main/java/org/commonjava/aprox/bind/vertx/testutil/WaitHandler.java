package org.commonjava.aprox.bind.vertx.testutil;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpClientResponse;

public class WaitHandler
    implements Handler<HttpClientResponse>
{
    private MultiMap responseHeaders;

    private int statusCode;

    private String statusMessage;

    @Override
    public synchronized void handle( final HttpClientResponse event )
    {
        this.statusCode = event.statusCode();
        this.statusMessage = event.statusMessage();
        this.responseHeaders = event.headers();
        notifyAll();
    }

    public MultiMap responseHeaders()
    {
        return responseHeaders;
    }

    public int statusCode()
    {
        return statusCode;
    }

    public String statusMessage()
    {
        return statusMessage;
    }
}