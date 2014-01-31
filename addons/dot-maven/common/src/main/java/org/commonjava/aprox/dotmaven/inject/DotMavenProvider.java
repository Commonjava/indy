package org.commonjava.aprox.dotmaven.inject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import net.sf.webdav.impl.ActivationMimeTyper;
import net.sf.webdav.impl.SimpleWebdavConfig;
import net.sf.webdav.spi.WebdavConfig;

import org.commonjava.aprox.dotmaven.store.DotMavenStore;
import org.commonjava.aprox.dotmaven.webctl.DotMavenService;
import org.commonjava.aprox.dotmaven.webctl.RequestInfo;

@ApplicationScoped
public class DotMavenProvider
{

    private WebdavConfig config;

    private DotMavenService service;

    @Inject
    private RequestInfo requestInfo;

    @Inject
    private DotMavenStore store;

    @Produces
    public DotMavenService getService()
    {
        if ( service == null )
        {
            service = new DotMavenService( getConfig(), store, new ActivationMimeTyper(), requestInfo );
        }

        return service;
    }

    @Produces
    public WebdavConfig getConfig()
    {
        if ( config == null )
        {
            config = new SimpleWebdavConfig().withLazyFolderCreationOnPut()
                                             .withoutOmitContentLengthHeader();
        }

        return config;
    }
}
