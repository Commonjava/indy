/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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

import groovy.lang.GroovyClassLoader;

import java.io.File;
import java.io.IOException;

import javax.enterprise.context.ApplicationScoped;

import org.codehaus.groovy.control.CompilationFailedException;

@ApplicationScoped
public class ScriptEngine
{

    private final GroovyClassLoader groovyClassloader = new GroovyClassLoader();

    public <T> T parseScriptInstance( final File script, final Class<T> type )
        throws IndyGroovyException
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
            throw new IndyGroovyException( "Failed to compile groovy script: '%s'. Reason: %s", e, script,
                                            e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new IndyGroovyException( "Failed to read groovy script: '%s'. Reason: %s", e, script, e.getMessage() );
        }
        catch ( final InstantiationException e )
        {
            throw new IndyGroovyException( "Cannot instantiate class parsed from script: '%s'. Reason: %s", e, script,
                                            e.getMessage() );
        }
        catch ( final IllegalAccessException e )
        {
            throw new IndyGroovyException( "Cannot instantiate class parsed from script: '%s'. Reason: %s", e, script,
                                            e.getMessage() );
        }
        catch ( final ClassCastException e )
        {
            throw new IndyGroovyException( "Script: '%s' instance: %s cannot be cast as: %s", e, script, instance,
                                            type.getName() );
        }
    }

    public <T> T parseScriptInstance( final String script, final Class<T> type )
        throws IndyGroovyException
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
            throw new IndyGroovyException( "Failed to compile groovy script: '%s'. Reason: %s", e, script,
                                            e.getMessage() );
        }
        catch ( final InstantiationException e )
        {
            throw new IndyGroovyException( "Cannot instantiate class parsed from script: '%s'. Reason: %s", e, script,
                                            e.getMessage() );
        }
        catch ( final IllegalAccessException e )
        {
            throw new IndyGroovyException( "Cannot instantiate class parsed from script: '%s'. Reason: %s", e, script,
                                            e.getMessage() );
        }
        catch ( final ClassCastException e )
        {
            throw new IndyGroovyException( "Script: '%s' instance: %s cannot be cast as: %s", e, script, instance,
                                            type.getName() );
        }
    }

}
