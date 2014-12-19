package org.commonjava.aprox.bind.vertx;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.commonjava.aprox.bind.vertx.testutil.PortFinder;
import org.commonjava.aprox.bind.vertx.testutil.WaitHandler;
import org.commonjava.aprox.core.bind.vertx.RestRouter;
import org.commonjava.vertx.vabr.ApplicationRouter;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.bind.filter.FilterCollection;
import org.commonjava.vertx.vabr.bind.route.RouteBinding;
import org.commonjava.vertx.vabr.bind.route.RouteCollection;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.commonjava.vertx.vabr.util.Respond;
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

public class Api10AliasingTest
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
        testRequest( "/api/1.0/test/handle" );
        assertThat( clientHandler.statusCode(), equalTo( 200 ) );

        final MultiMap headers = clientHandler.responseHeaders();
        for ( final Map.Entry<String, String> header : headers )
        {
            System.out.printf( "%s = %s\n", header.getKey(), header.getValue() );
        }
    }

    private void testRequest( final String requestPath )
        throws Exception
    {
        final TestHandler handler = new TestHandler();
        final Set<RequestHandler> handlers = Collections.<RequestHandler> singleton( handler );
        final List<RouteCollection> routeCollections =
            Collections.<RouteCollection> singletonList( new Collection( handler ) );

        server =
            vertx.createHttpServer()
                 .requestHandler( new RestRouter( handlers, routeCollections,
                                                  Collections.<FilterCollection> emptyList() ) )
                 .listen( port, host );

        final HttpClientRequest req = client.get( requestPath, clientHandler );

        req.end();

        synchronized ( clientHandler )
        {
            clientHandler.wait();
        }
    }

    @Handles( "/test" )
    public static final class TestHandler
        implements RequestHandler
    {
        @Route( path = "/handle", method = Method.GET )
        public void handle( final HttpServerRequest request )
        {
            Respond.to( request )
                   .ok()
                   .send();
        }
    }

    public static final class Collection
        implements RouteCollection
    {

        private final Binding binding;

        public Collection( final TestHandler handler )
            throws Exception
        {
            binding = new Binding( new BindingInfo(), handler );
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

        private String handlesPath;

        private String routePath;

        public BindingInfo()
            throws Exception
        {
            this.cls = TestHandler.class;
            this.methodName = "handle";
            final java.lang.reflect.Method method =
                cls.getMethod( methodName, new Class<?>[] { HttpServerRequest.class } );

            final Handles handles = cls.getAnnotation( Handles.class );
            final Route route = method.getAnnotation( Route.class );

            handlesPath = handles.value();
            if ( handlesPath == null || handlesPath.length() < 1 )
            {
                handlesPath = handles.prefix();
            }

            routePath = route.value();
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

        public String getRoutePath()
        {
            return routePath;
        }

        public String getHandlerPath()
        {
            return handlesPath;
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
        private final TestHandler handler;

        public Binding( final BindingInfo info, final TestHandler handler )
        {
            super( info.getPriority(), info.getPath(), info.getRoutePath(), info.getHandlerPath(), info.getMethod(),
                   info.getContentType(), info.getKey(),
                   info.getCls(), info.getMethodName(), Arrays.asList( info.getVersions() ) );
            this.handler = handler;
        }

        @Override
        protected void dispatch( final ApplicationRouter router, final HttpServerRequest req )
            throws Exception
        {
            handler.handle( req );
        }
    }

}
