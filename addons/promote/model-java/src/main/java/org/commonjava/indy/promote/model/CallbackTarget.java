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

    private final String url;

    private final CallbackMethod method;

    private final Map<String, String> headers;

    public CallbackTarget( String url, CallbackMethod method, Map<String, String> headers )
    {
        this.url = url;
        this.method = method;
        this.headers = headers;
    }

    public CallbackTarget( String url, CallbackMethod method )
    {
        this( url, method, Collections.emptyMap() );
    }
}
