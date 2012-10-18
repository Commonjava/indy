package org.commonjava.aprox.autoprox.inject;

import static org.apache.commons.io.IOUtils.closeQuietly;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.aprox.autoprox.conf.AutoProxConfiguration;
import org.commonjava.aprox.autoprox.conf.AutoProxModel;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.inject.Production;
import org.commonjava.web.json.ser.JsonSerializer;

@javax.enterprise.context.ApplicationScoped
public class AutoProxProvider
{
    @Inject
    private AutoProxConfiguration config;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    private AutoProxModel model;

    @SuppressWarnings( "unchecked" )
    @Produces
    @Production
    @Default
    public final AutoProxModel loadAutoProxModel()
        throws IOException
    {
        if ( model == null )
        {
            final String path = config.getPath();
            final File modelFile = new File( path );

            if ( !modelFile.exists() )
            {
                model = new AutoProxModel();
            }
            else
            {
                InputStream stream = null;
                try
                {
                    stream = new FileInputStream( modelFile );
                    model = serializer.fromStream( stream, "UTF-8", AutoProxModel.class );
                }
                finally
                {
                    closeQuietly( stream );
                }
            }
        }

        return model;
    }

}
