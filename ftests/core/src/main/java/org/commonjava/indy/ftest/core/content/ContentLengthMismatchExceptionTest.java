//package org.commonjava.indy.ftest.core.content;
//
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang.StringUtils;
//import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
//import org.commonjava.indy.model.core.Group;
//import org.commonjava.indy.model.core.RemoteRepository;
//import org.commonjava.maven.galley.model.Location;
//import org.commonjava.test.http.expect.ExpectationHandler;
//import org.junit.Test;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.ByteArrayInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.OutputStream;
//
//import static org.commonjava.indy.model.core.StoreType.group;
//import static org.commonjava.indy.model.core.StoreType.remote;
//import static org.hamcrest.CoreMatchers.notNullValue;
//import static org.hamcrest.core.IsNull.nullValue;
//import static org.junit.Assert.assertThat;
//
//public class ContentLengthMismatchExceptionTest
//                extends AbstractContentManagementTest
//{
//
//    protected final Logger logger = LoggerFactory.getLogger( getClass() );
//
//    private final static String expectContent = "target file's length is 10000k";
//
//    private final static String actualContent = "target file's length is 1k";
//
//    private static final String repo1 = "repo1";
//    private static final String path = " ";
//
//    private static int index = 0;
////    @Rule
////    public ExpectationServer server1 = new ExpectationServer();
//
//    @Test
//    public void runWithMismacthByRemoteRespository() throws Exception
//    {
//        final String repo1 = "repo1";
//        final String path = "org/foo/bar/maven-metadata.xml";
//
//        final String repo1Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<metadata>\n"
//                        + "  <groupId>org.foo</groupId>\n" + "  <artifactId>bar</artifactId>\n" + "  <versioning>\n"
//                        + "    <latest>1.0</latest>\n" + "    <release>1.0</release>\n" + "    <versions>\n"
//                        + "      <version>1.0</version>\n" + "    </versions>\n"
//                        + "    <lastUpdated>20150722164334</lastUpdated>\n" + "  </versioning>\n" + "</metadata>\n";
//
//        server.expect( "GET", server.formatUrl( STORE, path ), new MockExpectationHandler() );
//        //        server.expect( "GET" , server.formatUrl( repo1, path ),200 ,repo1Content);
//        RemoteRepository remote1 = new RemoteRepository( STORE, server.formatUrl( STORE ) );
//
//        remote1.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( -1 ) );
//        remote1 = client.stores().create( remote1, "adding remote", RemoteRepository.class );
//        assertThat(client.content().get( remote, STORE, path ) , nullValue() );
//        String result = IOUtils.toString( client.content().get( remote, STORE, path ) );
//        logger.info( "result :{}", result );
//        assertThat( result, notNullValue() );
//    }
//
////    @Test
//    public void runWithoutMismacthByRemoteRespository() throws Exception
//    {
//        byte[] bytes = expectContent.getBytes();
//        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
//        server.expect( server.formatUrl( STORE, path ), 200,bais );
//        RemoteRepository remote1 = new RemoteRepository( STORE, server.formatUrl( STORE ) );
//        remote1.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( -1 ) );
//        remote1 = client.stores()
//                        .create( remote1, "adding remote", RemoteRepository.class );
//        String result  = IOUtils.toString( client.content().get( remote, STORE, path ) );
//        org.junit.Assert.assertEquals( result.length(), expectContent.length());
//    }
//
////    @Test
//    public void runWithMismacthByGroup()
//                    throws Exception
//    {
//        final String repo1 = "repo1";
//        final String repo2 = "repo2";
//        final String path = "org/foo/bar/maven-metadata.xml";
//
//        final String repo1Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//                        "<metadata>\n" +
//                        "  <groupId>org.foo</groupId>\n" +
//                        "  <artifactId>bar</artifactId>\n" +
//                        "  <versioning>\n" +
//                        "    <latest>1.0</latest>\n" +
//                        "    <release>1.0</release>\n" +
//                        "    <versions>\n" +
//                        "      <version>1.0</version>\n" +
//                        "    </versions>\n" +
//                        "    <lastUpdated>20150722164334</lastUpdated>\n" +
//                        "  </versioning>\n" +
//                        "</metadata>\n";
//
//        final String repo2Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//                        "<metadata>\n" +
//                        "  <groupId>org.foo</groupId>\n" +
//                        "  <artifactId>bar</artifactId>\n" +
//                        "  <versioning>\n" +
//                        "    <latest>2.0</latest>\n" +
//                        "    <release>2.0</release>\n" +
//                        "    <versions>\n" +
//                        "      <version>2.0</version>\n" +
//                        "    </versions>\n" +
//                        "    <lastUpdated>20160722164334</lastUpdated>\n" +
//                        "  </versioning>\n" +
//                        "</metadata>\n";
//
//
//        server.expect( "GET" , server.formatUrl( repo2, path ),200 ,repo2Content);
//        server.expect( "GET" , server.formatUrl( repo1, path ), new MockExpectationHandler());
//        RemoteRepository remote1 = new RemoteRepository( repo1, server.formatUrl( repo1 ) );
//
//        remote1 = client.stores()
//                        .create( remote1, "adding remote1", RemoteRepository.class );
//
//        RemoteRepository remote2 = new RemoteRepository( repo2, server.formatUrl( repo2 ) );
//        remote2.setDisabled( true );
//
//        remote2 = client.stores()
//                        .create( remote2, "adding remote2", RemoteRepository.class );
//        Group g = new Group( "test", remote1.getKey(),remote2.getKey() );
//        g = client.stores()
//                  .create( g, "adding group", Group.class );
//
//        System.out.printf( "\n\nGroup constituents are:\n  %s\n\n", StringUtils.join( g.getConstituents(), "\n  " ) );
//        InputStream stream = null;
//        try
//        {
//            stream = client.content().get( group, g.getName(), path );
//        }catch ( Exception e )
//        {
//            int a = 0;
//        }
//        InputStream streamOne = null;
//        try
//        {
//            streamOne = client.content().get( group, g.getName(), path );
//        }catch ( Exception e )
//        {
//            int a1 = 0;
//        }
//
//        assertThat( streamOne, notNullValue() );
//
//        //        final String metadata = IOUtils.toString( stream );
//        //        assertThat( metadata, equalTo( repo1Content ) );w
//        stream.close();
//    }
//
//    class MockExpectationHandler implements ExpectationHandler
//    {
//
//        /* @formatter:off */
//        final static String repo1Content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
//            "<metadata>\n" +
//            "  <groupId>org.foo</groupId>\n" +
//            "  <artifactId>bar</artifactId>\n" +
//            "  <versioning>\n" +
//            "    <latest>1.0</latest>\n" +
//            "    <release>1.0</release>\n" +
//            "    <versions>\n" +
//            "      <version>1.0</version>\n" +
//            "    </versions>\n" +
//            "    <lastUpdated>20150722164334</lastUpdated>\n" +
//            "  </versioning>\n" +
//            "</metadata>\n";
//        @Override
//        public void handle( HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse )
//                        throws ServletException, IOException
//        {
//            httpServletResponse.setIntHeader(  "Content-Length", repo1Content.length()  );
//            httpServletResponse.setCharacterEncoding( "UTF-8" );
//            httpServletResponse.setContentType( "application/octet-stream; charset=utf-8" );
//
//            OutputStream out = httpServletResponse.getOutputStream();
//            if ( index < 1 )
//            {
//
//                try
//                {
//                    logger.info( "MockExpectationHandler call index ="+index+" url:"+ httpServletRequest.getRequestURI());
//                    httpServletResponse.setStatus( 200 );
//                    copy( new ExceptionInputStream( repo1Content.getBytes() ), out );
//                    ++index;
//                }catch ( Throwable t )
//                {
//                    throw new ServletException(t.getMessage());
//                }
//
//            }
//            else
//            {
//                logger.info( "MockExpectationHandler call index =  "+index+" url:"+ httpServletRequest.getRequestURI());
//                IOUtils.write( repo1Content, out );
//                httpServletResponse.setStatus( 200 );
//            }
//        }
//    }
//
//    static class ExceptionInputStream
//                    extends ByteArrayInputStream
//    {
//
//        public ExceptionInputStream( byte[] buf )
//        {
//            super( buf );
//        }
//
//        int index = 0;
//
//        @Override
//        public int read()
//        {
//            if ( this.pos < buf.length )
//            {
//                return super.read();
//
//            }
//
//            return -1;
//        }
//
//        @Override
//        public int read( byte[] buf ) throws IOException
//        {
//            if(index<2)
//            {
//                int result = -1;
//                result = super.read( buf );
//                index++;
//
//                return result;
//            }
//            return -1;
////            throw  new IOException("break it");
//        }
//
//    }
//
//    public static void copy( ExceptionInputStream in, OutputStream out) throws IOException
//    {
//        byte[] buffer = new byte[64];
//        while (true) {
//            int bytesRead = in.read(buffer);
//            if (bytesRead == -1)
//                break;
//            out.write(buffer, 0, bytesRead);
//        }
//    }
//
//}
//
