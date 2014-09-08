package org.commonjava.aprox.bind.vertx;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.http.auth.BasicUserPrincipal;
import org.commonjava.aprox.audit.SecuritySystem;
import org.commonjava.aprox.bind.vertx.util.SecurityParam;
import org.commonjava.vertx.vabr.anno.FilterRoute;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.bind.filter.ExecutionChain;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( key = "security" )
@ApplicationScoped
public class SecurityResource
    implements RequestHandler
{

    @Inject
    private SecuritySystem securitySystem;

    @FilterRoute( path = "/(?!.*(login|logout)).*", method = Method.ANY )
    public void setUser( final HttpServerRequest request, final ExecutionChain chain )
        throws Exception
    {
        // FIXME: Need a proper login!
        final String user = request.remoteAddress()
                                   .getHostString();

        securitySystem.setCurrentPrincipal( new BasicUserPrincipal( user ) );

        request.params()
               .add( SecurityParam.user.key(), user );

        chain.handle();
    }

}
