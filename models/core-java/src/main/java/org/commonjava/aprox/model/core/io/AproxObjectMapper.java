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
package org.commonjava.aprox.model.core.io;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.maven.atlas.ident.jackson.ProjectVersionRefSerializerModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Alternative
@Named
public class AproxObjectMapper
    extends ObjectMapper
{

    private static final long serialVersionUID = 1L;

    @Inject
    private Instance<Module> injectedModules;

    @Inject
    private Instance<ModuleSet> injectedModuleSets;

    private final Iterable<Module> ctorModules;

    private final Iterable<ModuleSet> ctorModuleSets;

    private Set<String> registeredModules = new HashSet<>();

    protected AproxObjectMapper()
    {
        ctorModules = null;
        ctorModuleSets = null;
    }

    public AproxObjectMapper( final boolean unused, final Module... additionalModules )
    {
        final Set<Module> mods = new HashSet<Module>();
        mods.add( ApiSerializerModule.INSTANCE );
        mods.add( ProjectVersionRefSerializerModule.INSTANCE );
        mods.addAll( Arrays.asList( additionalModules ) );
        this.ctorModules = mods;
        this.ctorModuleSets = null;

        init();
    }

    public AproxObjectMapper( final Iterable<Module> modules )
    {
        final Set<Module> mods = new HashSet<Module>();
        mods.add( new ApiSerializerModule() );
        mods.add( new ProjectVersionRefSerializerModule() );
        for ( final Module module : mods )
        {
            mods.add( module );
        }
        this.ctorModules = mods;
        this.ctorModuleSets = null;

        init();
    }

    public AproxObjectMapper( Instance<Module> modules, Instance<ModuleSet> moduleSets )
    {
        final Set<Module> mods = new HashSet<Module>();
        mods.add( new ApiSerializerModule() );
        mods.add( new ProjectVersionRefSerializerModule() );
        for ( final Module module : mods )
        {
            mods.add( module );
        }
        this.ctorModules = mods;
        this.ctorModuleSets = moduleSets;

        init();
    }

    @PostConstruct
    public void init()
    {
        setSerializationInclusion( Include.NON_EMPTY );
        configure( Feature.AUTO_CLOSE_JSON_CONTENT, true );

        enable( SerializationFeature.INDENT_OUTPUT, SerializationFeature.USE_EQUALITY_FOR_OBJECT_ID );

        enable( MapperFeature.AUTO_DETECT_FIELDS );
        //        disable( MapperFeature.AUTO_DETECT_GETTERS );

        disable( SerializationFeature.WRITE_NULL_MAP_VALUES, SerializationFeature.WRITE_EMPTY_JSON_ARRAYS );

        disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );

        inject( injectedModules, injectedModuleSets );
        inject( ctorModules, ctorModuleSets );
    }

    private void inject( Iterable<Module> modules, Iterable<ModuleSet> moduleSets )
    {
        Set<Module> injected = new HashSet<>();

        Logger logger = LoggerFactory.getLogger( getClass() );
        if ( modules != null )
        {
            for ( final Module module : modules )
            {
                injected.add( module );
            }
        }

        if ( moduleSets != null )
        {
            for ( ModuleSet moduleSet : moduleSets )
            {
                logger.debug("Adding module-set to object mapper..." );

                Set<Module> set = moduleSet.getModules();
                if ( set != null )
                {
                    for ( Module module : set )
                    {
                        injected.add( module );
                    }
                }
            }
        }

        for ( Module module : injected )
        {
            injectSingle( module );
        }

    }

    private void injectSingle( Module module )
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug("Registering object-mapper module: {}", module );

        registerModule( module );
        registeredModules.add( module.getClass().getSimpleName() );

        if ( module instanceof AproxSerializerModule )
        {
            ( (AproxSerializerModule) module ).register( this );
        }
    }

    public Set<String> getRegisteredModuleNames()
    {
        return registeredModules;
    }

    public String patchLegacyStoreJson( final String json )
        throws JsonProcessingException, IOException
    {
        final JsonNode tree = readTree( json );

        final JsonNode keyNode = tree.get( ArtifactStore.KEY_ATTR );
        final StoreKey key = StoreKey.fromString( keyNode.textValue() );
        if ( key == null )
        {
            throw new AproxSerializationException( "Cannot patch store JSON. No StoreKey 'key' attribute found!", null );
        }

        final JsonNode field = tree.get( ArtifactStore.TYPE_ATTR );
        if ( field == null )
        {
            ( (ObjectNode) tree ).put( ArtifactStore.TYPE_ATTR, key.getType()
                                                                   .singularEndpointName() );

            return writeValueAsString( tree );
        }

        return json;
    }

}
