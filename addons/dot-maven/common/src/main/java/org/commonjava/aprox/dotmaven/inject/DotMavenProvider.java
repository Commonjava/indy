package org.commonjava.aprox.dotmaven.inject;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import net.sf.webdav.impl.SimpleWebdavConfig;
import net.sf.webdav.spi.WebdavConfig;

@ApplicationScoped
public class DotMavenProvider
{

    private WebdavConfig config;

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
