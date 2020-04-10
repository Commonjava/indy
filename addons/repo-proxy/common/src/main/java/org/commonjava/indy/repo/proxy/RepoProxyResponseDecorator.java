package org.commonjava.indy.repo.proxy;

import org.commonjava.indy.model.core.StoreKey;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface RepoProxyResponseDecorator
{
    HttpServletResponse decoratingResponse( final HttpServletRequest request, final HttpServletResponse response,
                                            final StoreKey proxyToStoreKey )
            throws IOException;
}
