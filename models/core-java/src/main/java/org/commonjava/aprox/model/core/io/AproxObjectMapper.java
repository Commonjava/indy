package org.commonjava.aprox.model.core.io;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.commonjava.aprox.inject.Production;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Singleton
@Production
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

    public AproxObjectMapper( final Collection<Module> additionalModules )
    {
        final Set<Module> mods = new HashSet<Module>();
        mods.add( new ApiSerializerModule() );
        mods.add( new ProjectVersionRefSerializerModule() );
        mods.addAll( additionalModules );
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

}
