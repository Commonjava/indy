package org.commonjava.aprox.tensor.fixture;

import java.io.File;
import java.io.IOException;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.apache.commons.io.FileUtils;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.maven.atlas.effective.EProjectWeb;
import org.commonjava.maven.atlas.spi.effective.EGraphDriver;
import org.commonjava.maven.atlas.spi.neo4j.effective.FileNeo4JEGraphDriver;
import org.commonjava.util.logging.Logger;

@ApplicationScoped
public class TestTensorCoreProvider
{

    private EGraphDriver driver;

    private EProjectWeb globalWeb;

    private File dbDir;

    @Produces
    @Default
    @TestData
    public synchronized EProjectWeb getGlobalWeb()
        throws IOException
    {
        if ( globalWeb == null )
        {
            globalWeb = new EProjectWeb( null, getDriver() );
        }

        return globalWeb;
    }

    @Produces
    @Default
    @TestData
    public synchronized EGraphDriver getDriver()
        throws IOException
    {
        if ( driver == null )
        {
            dbDir = File.createTempFile( "tensor.", ".db" );
            dbDir.delete();
            dbDir.mkdirs();

            driver = new FileNeo4JEGraphDriver( dbDir, false );
        }

        return driver;
    }

    @PreDestroy
    public void shutdown()
    {
        if ( dbDir.exists() )
        {
            try
            {
                FileUtils.forceDelete( dbDir );
            }
            catch ( final IOException e )
            {
                new Logger( getClass() ).error( e.getMessage(), e );
            }
        }
    }

}
