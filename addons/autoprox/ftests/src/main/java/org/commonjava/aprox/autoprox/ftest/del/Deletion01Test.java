package org.commonjava.aprox.autoprox.ftest.del;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.model.core.dto.StoreListingDTO;
import org.junit.Test;

public class Deletion01Test
    extends AbstractAutoproxDeletionTest
{

    @Test
    public void deleteHostedRepoWithContent_RepoNotReCreatedWhenContentIsDeleted()
        throws Exception
    {
        final String named = "test";
        final String path = "path/to/foo.txt";
        final String content = "This is a test";

        client.content()
              .store( StoreType.hosted, named, path, new ByteArrayInputStream( content.getBytes() ) );

        final InputStream stream = client.content()
                                         .get( StoreType.hosted, named, path );

        final String retrieved = IOUtils.toString( stream );

        assertThat( retrieved, equalTo( content ) );

        client.stores()
              .delete( StoreType.hosted, named, "Removing test repo" );

        System.out.println( "Waiting for server events to clear..." );
        synchronized ( this )
        {
            wait( 3000 );
        }

        final StoreListingDTO<HostedRepository> repos = client.stores()
                                                              .listHostedRepositories();

        boolean found = false;
        for ( final HostedRepository repo : repos )
        {
            if ( repo.getName()
                       .equals( named ) )
            {
                found = true;
                break;
            }
        }

        assertThat( found, equalTo( false ) );
    }

}
