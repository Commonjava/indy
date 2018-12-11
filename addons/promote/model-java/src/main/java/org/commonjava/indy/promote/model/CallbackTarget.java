package org.commonjava.indy.promote.model;

import java.util.Collections;
import java.util.Map;

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

    private String authToken;

    private CallbackMethod method;

    private Map<String, String> headers;

    public CallbackTarget()
    {
    }

    public CallbackTarget( String url, String authToken, CallbackMethod method, Map<String, String> headers )
    {
        this.url = url;
        this.authToken = authToken;
        this.method = method;
        this.headers = headers;
    }

    public CallbackTarget( String url, String authToken, CallbackMethod method )
    {
        this( url, authToken, method, Collections.emptyMap() );
    }

    public CallbackTarget( String url, String authToken )
    {
        this( url, authToken, CallbackMethod.POST, Collections.emptyMap() );
    }

    public void setUrl( String url )
    {
        this.url = url;
    }

    public void setAuthToken( String authToken )
    {
        this.authToken = authToken;
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

    public String getAuthToken()
    {
        return authToken;
    }

    public CallbackMethod getMethod()
    {
        return method;
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }
}
