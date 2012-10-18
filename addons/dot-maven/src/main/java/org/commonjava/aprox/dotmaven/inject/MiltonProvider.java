package org.commonjava.aprox.dotmaven.inject;

import io.milton.config.HttpManagerBuilder;
import io.milton.http.HttpManager;
import io.milton.http.ResourceFactory;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;

@Singleton
public class MiltonProvider
{

    private HttpManager httpManager;

    @Inject
    private ResourceFactory resourceFactory;

    @Produces
    public HttpManager getHttpManager()
        throws ServletException
    {
        if ( httpManager == null )
        {
            final HttpManagerBuilder builder = new HttpManagerBuilder();
            builder.setMainResourceFactory( resourceFactory );

            httpManager = builder.buildHttpManager();
        }

        return httpManager;
    }

}
