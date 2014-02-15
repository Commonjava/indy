package org.commonjava.aprox.subsys.template;

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.groovy.control.CompilationFailedException;

@ApplicationScoped
public class ScriptEngine
{

    private final GroovyClassLoader groovyClassloader = new GroovyClassLoader();

    // TODO: Cache parsed classes, though this will hurt hot-reloading...maybe create a debug mode configuration?
    public <T> T parseScriptInstance( final File script, final Class<T> type )
        throws AproxGroovyException
    {
        Object instance = null;
        try
        {
            final Class<?> clazz = groovyClassloader.parseClass( script );
            instance = clazz.newInstance();

            return type.cast( instance );
        }
        catch ( final CompilationFailedException e )
        {
            throw new AproxGroovyException( "Failed to compile groovy script: '%s'. Reason: %s", e, script, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxGroovyException( "Failed to read groovy script: '%s'. Reason: %s", e, script, e.getMessage() );
        }
        catch ( final InstantiationException e )
        {
            throw new AproxGroovyException( "Cannot instantiate class parsed from script: '%s'. Reason: %s", e, script, e.getMessage() );
        }
        catch ( final IllegalAccessException e )
        {
            throw new AproxGroovyException( "Cannot instantiate class parsed from script: '%s'. Reason: %s", e, script, e.getMessage() );
        }
        catch ( final ClassCastException e )
        {
            throw new AproxGroovyException( "Script: '%s' instance: %s cannot be cast as: %s", e, script, instance, type.getName() );
        }
    }

}
