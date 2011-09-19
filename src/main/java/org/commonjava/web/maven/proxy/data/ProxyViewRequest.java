package org.commonjava.web.maven.proxy.data;

import org.commonjava.couch.db.model.ViewRequest;
import org.commonjava.web.maven.proxy.conf.ProxyConfiguration;
import org.commonjava.web.maven.proxy.data.ProxyAppDescription.View;

public class ProxyViewRequest
    extends ViewRequest
{

    public ProxyViewRequest( final ProxyConfiguration config, final View view )
    {
        super( ProxyAppDescription.APP_NAME, view.viewName() );
        setParameter( INCLUDE_DOCS, true );
    }

    public ProxyViewRequest( final ProxyConfiguration config, final View view, final String key )
    {
        this( config, view );
        setParameter( KEY, key );
    }

}
