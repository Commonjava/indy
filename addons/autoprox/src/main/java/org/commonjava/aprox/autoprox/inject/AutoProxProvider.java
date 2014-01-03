/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
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

    @Produces
    @Production
    @Default
    public AutoProxModel loadAutoProxModel()
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
