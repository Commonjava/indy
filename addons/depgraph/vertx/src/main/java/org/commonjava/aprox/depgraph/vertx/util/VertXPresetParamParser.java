package org.commonjava.aprox.depgraph.vertx.util;

import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.aprox.depgraph.util.PresetParameterParser;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.cartographer.preset.CommonPresetParameters;

@ApplicationScoped
public class VertXPresetParamParser
    implements PresetParameterParser
{

    @Override
    public Map<String, Object> parse( final Map<String, String[]> params )
    {
        final Map<String, Object> result = new HashMap<String, Object>();

        String[] vals = params.get( "q:" + CommonPresetParameters.SCOPE );
        if ( vals == null || vals.length < 1 )
        {
            vals = params.get( "q:s" );
        }

        if ( vals != null && vals.length > 0 )
        {
            result.put( CommonPresetParameters.SCOPE, DependencyScope.getScope( vals[0] ) );
        }

        vals = params.get( "q:" + CommonPresetParameters.MANAGED );
        if ( vals == null || vals.length < 1 )
        {
            vals = params.get( "q:m" );
        }

        if ( vals != null && vals.length > 0 )
        {
            result.put( CommonPresetParameters.MANAGED, Boolean.valueOf( vals[0] ) );
        }

        return result;
    }

}
