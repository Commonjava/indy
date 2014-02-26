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
        throws RenderingException
    {
        final Template template = getTemplate( templateKey );

        final Writable output = template.make( params );

        final StringWriter writer = new StringWriter();
        try
        {
            output.writeTo( writer );
        }
        catch ( final IOException e )
        {
            throw new RenderingException( "Failed to render template: {}. Reason: {}", e, templateKey, e.getMessage() );
        }

        return writer.toString();
    }

    private Template getTemplate( final String templateKey )
        throws RenderingException
    {
        try
        {
            final String filename = templateKey + ".groovy";
            final File templateDir = config.getStorageDir( TEMPLATES );

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
                                    .getResource( TEMPLATES + "/" + templateKey + ".groovy" );

                template = u == null ? null : engine.createTemplate( u );
            }

            if ( template == null )
            {
                throw new RenderingException( "Failed to locate template: {}", templateKey );
            }

            return template;
        }
        catch ( final CompilationFailedException e )
        {
            throw new RenderingException( "Failed to compile template: {}. Reason: {}", e, templateKey, e.getMessage() );
        }
        catch ( final ClassNotFoundException e )
        {
            throw new RenderingException( "Failed to compile template: {}. Reason: {}", e, templateKey, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new RenderingException( "Failed to read template: {}. Reason: {}", e, templateKey, e.getMessage() );
        }
    }
}
