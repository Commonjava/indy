/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.subsys.template;

import groovy.lang.Writable;
import groovy.text.GStringTemplateEngine;
import groovy.text.Template;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codehaus.groovy.control.CompilationFailedException;
import org.commonjava.aprox.subsys.flatfile.conf.FlatFileConfiguration;

@ApplicationScoped
public class TemplatingEngine
{

    public static final String TEMPLATES = "templates";

    @Inject
    private FlatFileConfiguration config;

    private final GStringTemplateEngine engine;

    protected TemplatingEngine()
    {
        engine = new GStringTemplateEngine();
    }

    public TemplatingEngine( final GStringTemplateEngine engine, final FlatFileConfiguration config )
    {
        this.engine = engine;
        this.config = config;
    }

    public String render( final String templateKey, final Map<String, Object> params )
        throws AproxGroovyException
    {
        return render( null, templateKey, params );
    }

    public String render( final String acceptHeader, final String templateKey, final Map<String, Object> params )
        throws AproxGroovyException
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
            throw new AproxGroovyException( "Failed to render template: %s for accept: %s. Reason: %s", e, templateKey,
                                            acceptHeader, e.getMessage() );
        }

        return writer.toString();
    }

    // TODO Cache these...though this will hurt hot-reloading. Perhaps a debug mode configuration?
    private Template getTemplate( final String acceptHeader, final String templateKey )
        throws AproxGroovyException
    {
        final String accept = ( acceptHeader == null ? "" : acceptHeader.replace( '/', '_' ) ) + "/";
        try
        {
            final String filename = accept + templateKey + ".groovy";
            final File templateDir = config.getDataDir( TEMPLATES );

            final File templateFile = new File( templateDir, filename );

            Template template;
            if ( templateFile.exists() && !templateFile.isDirectory() )
            {
                template = engine.createTemplate( templateFile );
            }
            else
            {
                final URL u = Thread.currentThread()
                                    .getContextClassLoader()
                                    .getResource( TEMPLATES + "/" + accept + templateKey + ".groovy" );

                template = u == null ? null : engine.createTemplate( u );
            }

            if ( template == null )
            {
                throw new AproxGroovyException( "Failed to locate template: %s", templateKey );
            }

            return template;
        }
        catch ( final CompilationFailedException e )
        {
            throw new AproxGroovyException( "Failed to compile template: %s. Reason: %s", e, templateKey, e.getMessage() );
        }
        catch ( final ClassNotFoundException e )
        {
            throw new AproxGroovyException( "Failed to compile template: %s. Reason: %s", e, templateKey, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxGroovyException( "Failed to read template: %s. Reason: %s", e, templateKey, e.getMessage() );
        }
    }
}
