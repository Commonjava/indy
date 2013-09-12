package org.commonjava.aprox.depgraph.inject;

import org.commonjava.web.json.ser.WebSerializationAdapter;

import com.google.gson.GsonBuilder;

public class PrettyPrintAdapter
    implements WebSerializationAdapter
{

    @Override
    public void register( final GsonBuilder gsonBuilder )
    {
        gsonBuilder.setPrettyPrinting();
    }

}
