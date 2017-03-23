package org.commonjava.indy.relate.ftest;

import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.commonjava.indy.model.core.StoreType.hosted;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by ruhan on 2/17/17.
 */
public class PomUploadListenerTest
                extends AbstractIndyFunctionalTest
{
    private static final String path = "org/foo/bar/1/bar-1.pom";

    private static final String pathRel = path + ".rel";

    private static final String content =
                    "<project><modelVersion>4.0.0</modelVersion><groupId>org.foo</groupId><artifactId>bar</artifactId><version>1</version></project>";

    @Test
    public void run() throws Exception
    {
        final String repo1 = "repo1";

        HostedRepository hosted1 = new HostedRepository( repo1 );
        client.stores().create( hosted1, "adding hosted", HostedRepository.class );

        StoreKey key = new StoreKey( hosted, repo1 );
        InputStream stream = new ByteArrayInputStream( content.getBytes() );
        client.content().store( key, path, stream );

        waitForEventPropagation();

        boolean exists = client.content().exists( hosted, repo1, pathRel, true );
        assertThat( exists, equalTo( true ) );
    }
}
