package org.commonjava.aprox.autoprox.ftest.del;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.StoreListingDTO;
import org.junit.Test;

public class Deletion02Test
    extends AbstractAutoproxDeletionTest
{

    @Test
    public void deleteRepoWithContent_RepoNotReCreatedWhenContentIsDeleted()
        throws Exception
    {
        final String named = "test";
        final String path = "path/to/foo.txt";
        final String content = "This is a test";

        expectRepoAutoCreation( named );
        http.expect( http.formatUrl( named, path ), 200, content );

        final InputStream stream = client.content()
                                         .get( StoreType.remote, named, path );

        final String retrieved = IOUtils.toString( stream );

        assertThat( retrieved, equalTo( content ) );

        client.stores()
              .delete( StoreType.remote, named, "Removing test repo" );

        System.out.println( "Waiting for server events to clear..." );
        synchronized ( this )
        {
            wait( 3000 );
        }

        final StoreListingDTO<RemoteRepository> remotes = client.stores()
                                                                .listRemoteRepositories();

        boolean found = false;
        for ( final RemoteRepository remote : remotes )
        {
            if ( remote.getName()
                       .equals( named ) )
            {
                found = true;
                break;
            }
        }

        assertThat( found, equalTo( false ) );
    }

}
