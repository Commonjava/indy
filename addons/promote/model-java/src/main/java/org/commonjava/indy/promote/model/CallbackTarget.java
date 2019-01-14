package org.commonjava.indy.promote.model;

import java.util.Collections;
import java.util.Map;

import static org.commonjava.indy.promote.model.CallbackTarget.CallbackMethod.POST;

/**
 * Created by ruhan on 12/6/18.
 */
public class CallbackTarget
{
    public enum CallbackMethod
    {
        POST, PUT;
    }

    private String url;

    private CallbackMethod method;

    private Map<String, String> headers; // e.g., put( "Authorization", "Bearer ..." )

    public CallbackTarget()
    {
    }

    public CallbackTarget( String url, CallbackMethod method, Map<String, String> headers )
    {
        this.url = url;
        this.method = method;
        this.headers = headers;
    }

    public CallbackTarget( String url, Map<String, String> headers )
    {
        this( url, POST, headers );
    }

    public CallbackTarget( String url )
    {
        this( url, POST, Collections.emptyMap() );
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public void setMethod( CallbackMethod method )
    {
        this.method = method;
    }

    public void setHeaders( Map<String, String> headers )
    {
        this.headers = headers;
    }

    public String getUrl()
    {
        return url;
    }

    public CallbackMethod getMethod()
    {
        return method;
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }

    @Override
    public String toString()
    {
        return "CallbackTarget{" + "url='" + url + '\'' + ", method=" + method + '}';
    }
}
