package org.commonjava.aprox.client.core.module;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.aprox.client.core.AproxClientModule;
import org.commonjava.aprox.model.core.io.AproxObjectMapper;

/**
 * Created by jdcasey on 8/17/15.
 */
public class AproxRawObjectMapperModule
    extends AproxClientModule
{

    public AproxObjectMapper getObjectMapper()
    {
        return getHttp().getObjectMapper();
    }
}
