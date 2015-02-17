package org.commonjava.aprox.bind.vertx;

import javax.enterprise.context.ApplicationScoped;

import org.commonjava.aprox.bind.vertx.util.SecurityParam;
import org.commonjava.vertx.vabr.anno.FilterRoute;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.bind.filter.ExecutionChain;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.ApplicationHeader;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( key = "security" )
@ApplicationScoped
public class SecurityResource
    implements RequestHandler
{

    @FilterRoute( path = "/(?!.*(login|logout)).*", method = Method.ANY )
    public void setUser( final HttpServerRequest request, final ExecutionChain chain )
        throws Exception
    {
        // FIXME: Need a proper login!
        String user = request.headers()
                             .get( ApplicationHeader.x_forwarded_for.key() );
        if ( user == null )
        {
            user = request.remoteAddress()
                          .getHostString();
        }

        request.params()
               .add( SecurityParam.user.key(), user );

        chain.handle();
    }

    @Route( "/whoami" )
    public void whoami( final HttpServerRequest request )
    {
        Respond.to( request )
               .ok()
               .entity( request.params()
                               .get( SecurityParam.user.key() ) )
               .send();
    }

}
