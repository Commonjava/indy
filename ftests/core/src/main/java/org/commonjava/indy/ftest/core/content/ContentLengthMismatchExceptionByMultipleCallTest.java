package org.commonjava.indy.ftest.core.content;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.commonjava.indy.ftest.core.AbstractContentManagementTest;
import org.commonjava.indy.ftest.core.fixture.ContenlengthExpectationHandlerExecutor;
import org.commonjava.indy.ftest.core.fixture.ExpectationHandlerExecutor;
import org.commonjava.indy.ftest.core.fixture.MockExpectationHandler;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.maven.galley.model.Location;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

/**
 * The test means, if indy download file from remote repo is not correct (lose some byte),Indy will
 * delete the target file and throw TransferContentException.
 * Then client can download it again.
 * <b>GIVEN:</b>
 * <ul>
 *     <li>A remote repo</li>
 *     <li>remote file: maven-metadata.xml</li>
 * </ul>
 *
 * <br/>
 * <b>WHEN:</b>
 * <ul>
 *     <li>first download the file, the case will break the outPutStream and throw Exception and delete the target file(incorrect file)</li>
 *     <li>second download the file, the case can return correct file </li>
 * </ul>
 *
 * <br/>
 * <b>THEN:</b>
 * <ul>
 *     <li>When first download the file, the case can just get null/li>
 *     <li>When second download the file, the case can get correct file</li>
 * </ul>
 */
public class ContentLengthMismatchExceptionByMultipleCallTest
                extends AbstractContentManagementTest
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );


    private MockExpectationHandler handler;

    private ExpectationHandlerExecutor exectuor;

    private final static String responseContent =
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<metadata>\n" + "  <groupId>org.foo</groupId>\n"
                                    + "  <artifactId>bar</artifactId>\n" + "  <versioning>\n"
                                    + "    <latest>1.0</latest>\n" + "    <release>1.0</release>\n" + "    <versions>\n"
                                    + "      <version>1.0</version>\n" + "    </versions>\n"
                                    + "    <lastUpdated>20150722164334</lastUpdated>\n" + "  </versioning>\n"
                                    + "</metadata>\n";

    @Before
    public void setup()
    {

        exectuor = new ContenlengthExpectationHandlerExecutor( 0, 1, responseContent );
        MockExpectationHandler.Builder builder = MockExpectationHandler.getBuilder();
        handler = builder.characterEncoding( "UTF-8" )
                         .intHeaders( ImmutablePair.of( "Content-Length", responseContent.length() ) )
                         .expectationHandlerExecutor( exectuor )
                         .contentType( "application/octet-stream; charset=utf-8" )
                         .status( 200 )
                         .build();
    }

    @Test
    public void runWithMismacthByRemoteRespository() throws Exception
    {
        final String path = "org/foo/bar/maven-metadata.xml";

        server.expect( "GET", server.formatUrl( STORE, path ), handler );
        RemoteRepository remote = new RemoteRepository( STORE, server.formatUrl( STORE ) );

        remote.setMetadata( Location.CONNECTION_TIMEOUT_SECONDS, Integer.toString( -1 ) );
        remote = client.stores().create( remote, "adding remote", RemoteRepository.class );
        assertThat( client.content().get( StoreType.remote, STORE, path ), nullValue() );
        String result = IOUtils.toString( client.content().get( StoreType.remote, STORE, path ) );
        logger.info( "runWithMismacthByRemoteRespository ---- result :{}", result );
        assertThat( result, notNullValue() );
    }
}
