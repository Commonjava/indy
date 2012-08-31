package org.commonjava.aprox.stats;

import javax.inject.Singleton;

import org.commonjava.web.json.ser.JsonAdapters;

import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;

@Singleton
@JsonAdapters( AProxVersioningAdapter.class )
public class AProxVersioning
{
    
    private static final String APP_VERSION = "@project.version@";
    private static final String APP_BUILDER = "@user.name@";
    private static final String APP_COMMIT_ID = "@buildNumber@";
    private static final String APP_TIMESTAMP = "@timestamp@";
    
    public String getVersion()
    {
        return APP_VERSION;
    }

    public String getBuilder()
    {
        return APP_BUILDER;
    }

    public String getCommitId()
    {
        return APP_COMMIT_ID;
    }
    
    public String getTimestamp()
    {
        return APP_TIMESTAMP;
    }

}
