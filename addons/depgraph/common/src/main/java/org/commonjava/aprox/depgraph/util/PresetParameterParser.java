package org.commonjava.aprox.depgraph.util;

import java.util.Map;

public interface PresetParameterParser
{

    Map<String, Object> parse( Map<String, String[]> requestParams );

}
