package org.commonjava.indy.metrics.socket;

import org.commonjava.indy.metrics.zabbix.sender.SenderResult;

/**
 * Created by xiabai on 5/9/17.
 */
public class Expectation
{
    private final String method;

    private final byte[] requestBody;

    private final SenderResult senderResult;

    public Expectation( String method, byte[] requestBody, SenderResult senderResult )
    {
        this.method = method;
        this.requestBody = requestBody;
        this.senderResult = senderResult;
    }

    public String getMethod()
    {
        return method;
    }

    public byte[] getRequestBody()
    {
        return requestBody;
    }

    public SenderResult getSenderResult()
    {
        return senderResult;
    }
}
