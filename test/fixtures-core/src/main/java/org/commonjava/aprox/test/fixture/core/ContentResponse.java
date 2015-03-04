package org.commonjava.aprox.test.fixture.core;

public final class ContentResponse
{
    private final int code;

    private final String body;

    private final String path;

    private final String method;

    ContentResponse( final String method, final String path, final int code, final String body )
    {
        this.method = method;
        this.path = path;
        this.code = code;
        this.body = body;
    }

    public String method()
    {
        return method;
    }

    public String path()
    {
        return path;
    }

    public int code()
    {
        return code;
    }

    public String body()
    {
        return body;
    }

    @Override
    public String toString()
    {
        return "Expect (" + method + " " + path + "), and respond with code:" + code() + ", body:\n" + body();
    }
}