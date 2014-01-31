package org.commonjava.aprox.dotmaven.inject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import net.sf.webdav.impl.ActivationMimeTyper;
import net.sf.webdav.impl.SimpleWebdavConfig;
import net.sf.webdav.spi.IMimeTyper;
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

    private IMimeTyper mimeTyper;

    @Produces
    public DotMavenService getService()
    {
        if ( service == null )
        {
            service = new DotMavenService( getConfig(), store, getMimeTyper(), requestInfo );
        }

        return service;
    }

    @Produces
    public synchronized IMimeTyper getMimeTyper()
    {
        if ( mimeTyper == null )
        {
            mimeTyper = new ActivationMimeTyper();
        }

        return mimeTyper;
    }

    @Produces
    public synchronized WebdavConfig getConfig()
    {
        if ( config == null )
        {
            config = new SimpleWebdavConfig().withLazyFolderCreationOnPut()
                                             .withoutOmitContentLengthHeader();
        }

        return config;
    }
}
