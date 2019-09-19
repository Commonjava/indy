/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.subsys.template;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codehaus.groovy.control.CompilationFailedException;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class TemplatingEngine
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    public static final String TEMPLATES = "templates";

    @Inject
    private DataFileManager manager;

    private final GStringTemplateEngine engine;

    protected TemplatingEngine()
    {
        engine = new GStringTemplateEngine();
    }

    public TemplatingEngine( final GStringTemplateEngine engine, final DataFileManager manager )
    {
        this.engine = engine;
        this.manager = manager;
    }

    public String render( final String templateKey, final Map<String, Object> params )
        throws IndyGroovyException
    {
        return render( null, templateKey, params );
    }

    public String render( final String acceptHeader, final String templateKey, final Map<String, Object> params )
        throws IndyGroovyException
    {
        final Template template = getTemplate( acceptHeader, templateKey );

        final Writable output = template.make( params );

        final StringWriter writer = new StringWriter();
        try
        {
            output.writeTo( writer );
        }
        catch ( final IOException e )
        {
            throw new IndyGroovyException( "Failed to render template: %s for addMetadata: %s. Reason: %s", e, templateKey,
                                            acceptHeader, e.getMessage() );
        }

        return writer.toString();
    }

    // TODO Cache these...though this will hurt hot-reloading. Perhaps a debug mode configuration?
    private Template getTemplate( final String acceptHeader, final String templateKey )
        throws IndyGroovyException
    {
        final String accept = ( acceptHeader == null ? "" : acceptHeader.replace( '/', '_' ) + "/" );
        try
        {
            final String filename = accept + templateKey + ".groovy";
            final DataFile templateFile = manager.getDataFile( TEMPLATES, filename );
            logger.info( "Looking for template: {} for ACCEPT header: {} in: {}", templateKey, acceptHeader,
                         templateFile );

            Template template;
            if ( templateFile.exists() && !templateFile.isDirectory() )
            {
                template = engine.createTemplate( templateFile.readString() );
            }
            else
            {
                final String urlpath = TEMPLATES + "/" + accept + templateKey + ".groovy";
                logger.info( "Looking for template: {} for ACCEPT header: {} in: {}", templateKey, acceptHeader,
                             urlpath );

                final URL u = Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResource( urlpath );

                template = u == null ? null : engine.createTemplate( u );
            }

            if ( template == null )
            {
                throw new IndyGroovyException( "Failed to locate template: %s (with ACCEPT header: %s)", templateKey,
                                                acceptHeader );
            }

            return template;
        }
        catch ( final CompilationFailedException e )
        {
            throw new IndyGroovyException( "Failed to compile template: %s. Reason: %s", e, templateKey, e.getMessage() );
        }
        catch ( final ClassNotFoundException e )
        {
            throw new IndyGroovyException( "Failed to compile template: %s. Reason: %s", e, templateKey, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new IndyGroovyException( "Failed to read template: %s. Reason: %s", e, templateKey, e.getMessage() );
        }
    }
}
