package org.commonjava.indy.boot.jaxrs;

import io.undertow.Undertow;
import org.commonjava.indy.conf.IndyConfigInfo;
import org.commonjava.web.config.annotation.ConfigName;
import org.commonjava.web.config.annotation.SectionName;

import javax.enterprise.context.ApplicationScoped;
import java.io.File;
import java.io.InputStream;

@SectionName( "rest")
@ApplicationScoped
public class RestConfig
        implements IndyConfigInfo
{
    private Integer ioThreads;

    private Integer workerThreads;

    @Override
    public String getDefaultConfigFileName()
    {
        return new File( IndyConfigInfo.CONF_INCLUDES_DIR, "rest.conf" ).getPath();
    }

    @Override
    public InputStream getDefaultConfig()
    {
        return Thread.currentThread()
                     .getContextClassLoader()
                     .getResourceAsStream( "default-rest.conf" );
    }

    public Integer getIoThreads()
    {
        return ioThreads;
    }

    @ConfigName( "io.threads" )
    public void setIoThreads( final Integer ioThreads )
    {
        this.ioThreads = ioThreads;
    }

    public Integer getWorkerThreads()
    {
        return workerThreads;
    }

    @ConfigName( "worker.threads" )
    public void setWorkerThreads( final Integer workerThreads )
    {
        this.workerThreads = workerThreads;
    }

    public void configureBuilder( final Undertow.Builder builder )
    {
        if ( ioThreads != null )
        {
            builder.setIoThreads( ioThreads );
        }

        if ( workerThreads != null )
        {
            builder.setWorkerThreads( workerThreads );
        }
    }
}
