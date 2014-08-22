package org.commonjava.aprox.bind.vertx;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.commonjava.aprox.bind.vertx.testutil.PortFinder;
import org.commonjava.aprox.bind.vertx.testutil.WaitHandler;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.ApplicationRouterConfig;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.bind.route.RouteBinding;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.commonjava.vertx.vabr.types.Method;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.Vertx;
import org.vertx.java.core.http.HttpClient;
import org.vertx.java.core.http.HttpClientRequest;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.impl.DefaultVertx;

public class DeprecatedApi10RedirectorTest
{

    private HttpServer server;

    private HttpClient client;

    private Vertx vertx;

    private WaitHandler clientHandler;

    private int port;

    private String host;

    @Before
    public void setup()
    {
        vertx = new DefaultVertx();

        port = PortFinder.findOpenPort();
        host = "127.0.0.1";

        client = vertx.createHttpClient()
                      .setHost( host )
                      .setPort( port );

        clientHandler = new WaitHandler();
    }

    @After
    public void shutdown()
    {
        if ( client != null )
        {
            client.close();
        }

        if ( server != null )
        {
            server.close();
        }
    }

    @Test
    public void test()
        throws Exception
    {
        final MultiMap headers = getResponseHeaders( "/api/1.0/status/addons/active.js" );
        final String location = headers.get( ApplicationHeader.location.key() );

        for ( final Map.Entry<String, String> entry : headers )
        {
            System.out.printf( "%s = %s\n", entry.getKey(), entry.getValue() );
        }

        assertThat( location, equalTo( "/api/status/addons/active.js" ) );
    }

    private MultiMap getResponseHeaders( final String requestPath )
        throws Exception
    {
        final DeprecatedApi10Redirector redirector = new DeprecatedApi10Redirector();

        server =
            vertx.createHttpServer()
                 .requestHandler( new ApplicationRouter(
                                                         new ApplicationRouterConfig().withPrefix( "/api" )
                                                                                      .withHandler( redirector )
                                                                                      .withRouteCollection( new Collection(
                                                                                                                            redirector ) ) ) )
                 .listen( port, host );

        final HttpClientRequest req = client.get( requestPath, clientHandler );

        req.end();

        synchronized ( clientHandler )
        {
            clientHandler.wait();
        }

        return clientHandler.responseHeaders();
    }

    public static final class Collection
        implements RouteCollection
    {

        private final Binding binding;

        public Collection( final DeprecatedApi10Redirector redirector )
            throws Exception
        {
            binding = new Binding( new BindingInfo(), redirector );
        }

        @Override
        public Iterator<RouteBinding> iterator()
        {
            return Collections.<RouteBinding> singleton( binding )
                              .iterator();
        }

        @Override
        public Set<RouteBinding> getRoutes()
        {
            return Collections.<RouteBinding> singleton( binding );
        }

    }

    public static final class BindingInfo
    {
        private final Class<?> cls;

        private final String path;

        private final Method method;

        private final int priority;

        private final String contentType;

        private final String[] versions;

        private final String key;

        private final String methodName;

        public BindingInfo()
            throws Exception
        {
            this.cls = DeprecatedApi10Redirector.class;
            this.methodName = "handle";
            final java.lang.reflect.Method method =
                cls.getMethod( methodName, new Class<?>[] { HttpServerRequest.class } );

            final Handles handles = cls.getAnnotation( Handles.class );
            final Route route = method.getAnnotation( Route.class );

            String handlesPath = handles.value();
            if ( handlesPath == null || handlesPath.length() < 1 )
            {
                handlesPath = handles.prefix();
            }

            String routePath = route.value();
            if ( routePath == null || routePath.length() < 1 )
            {
                routePath = route.path();
            }

            final StringBuilder p = new StringBuilder();
            if ( handlesPath != null && handlesPath.length() > 0 )
            {
                p.append( handlesPath );
            }

            if ( routePath != null && routePath.length() > 0 )
            {
                p.append( routePath );
            }

            this.path = p.toString();
            this.method = route.method();
            this.priority = route.priority();
            this.contentType = route.contentType();
            this.versions = route.versions();
            this.key = handles.key();
        }

        public Class<?> getCls()
        {
            return cls;
        }

        public String getPath()
        {
            return path;
        }

        public Method getMethod()
        {
            return method;
        }

        public int getPriority()
        {
            return priority;
        }

        public String getContentType()
        {
            return contentType;
        }

        public String[] getVersions()
        {
            return versions;
        }

        public String getKey()
        {
            return key;
        }

        public String getMethodName()
        {
            return methodName;
        }

    }

    public static final class Binding
        extends RouteBinding
    {
        private final DeprecatedApi10Redirector redirector;

        public Binding( final BindingInfo info, final DeprecatedApi10Redirector redirector )
        {
            super( info.getPriority(), info.getPath(), info.getMethod(), info.getContentType(), info.getKey(),
                   info.getCls(), info.getMethodName(), Arrays.asList( info.getVersions() ) );
            this.redirector = redirector;
        }

        @Override
        protected void dispatch( final ApplicationRouter router, final HttpServerRequest req )
            throws Exception
        {
            redirector.handle( req );
        }
    }

}
