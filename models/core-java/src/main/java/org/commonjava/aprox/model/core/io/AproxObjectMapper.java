package org.commonjava.aprox.model.core.io;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Alternative
@Named
public class AproxObjectMapper
    extends ObjectMapper
{

    private static final long serialVersionUID = 1L;

    @Inject
    private Instance<Module> injectedModules;

    private final Iterable<Module> ctorModules;

    protected AproxObjectMapper()
    {
        ctorModules = null;
    }

    public AproxObjectMapper( final boolean unused, final Module... additionalModules )
    {
        final Set<Module> mods = new HashSet<Module>();
        mods.add( new ApiSerializerModule() );
        mods.add( new ProjectVersionRefSerializerModule() );
        mods.addAll( Arrays.asList( additionalModules ) );
        this.ctorModules = mods;

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

        if ( injectedModules != null )
        {
            registerModules( injectedModules );
            for ( final Module module : injectedModules )
            {
                if ( module instanceof AproxSerializerModule )
                {
                    ( (AproxSerializerModule) module ).register( this );
                }
            }
        }

        if ( ctorModules != null )
        {
            registerModules( ctorModules );
            for ( final Module module : ctorModules )
            {
                if ( module instanceof AproxSerializerModule )
                {
                    ( (AproxSerializerModule) module ).register( this );
                }
            }
        }
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
