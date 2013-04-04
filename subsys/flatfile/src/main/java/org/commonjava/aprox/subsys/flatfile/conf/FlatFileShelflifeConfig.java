package org.commonjava.aprox.subsys.flatfile.conf;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.commonjava.shelflife.store.flat.FlatBlockStoreConfiguration;

@ApplicationScoped
public class FlatFileShelflifeConfig
{

    @Inject
    private FlatFileConfiguration config;

    @Produces
    @Default
    public FlatBlockStoreConfiguration getShelflifeConfig()
    {
        return new FlatBlockStoreConfiguration( config.getStorageDir( "shelflife" ) );
    }

}
