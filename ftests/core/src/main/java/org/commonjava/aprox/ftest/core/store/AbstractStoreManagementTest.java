package org.commonjava.aprox.ftest.core.store;

import static org.apache.commons.lang.StringUtils.join;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.dto.StoreListingDTO;

public class AbstractStoreManagementTest
    extends AbstractAproxFunctionalTest
{

    protected final void checkListing( final StoreListingDTO<? extends ArtifactStore> dto,
                                     final Set<ArtifactStore> expected, final List<Set<ArtifactStore>> banned )
    {
        final List<? extends ArtifactStore> stores = dto.getItems();

        for ( final ArtifactStore store : expected )
        {
            assertThat( store.getKey() + " should be present in:\n  " + join( keys( stores ), "\n  " ),
                        stores.contains( store ),
                        equalTo( true ) );
        }

        for ( final Set<ArtifactStore> bannedSet : banned )
        {
            for ( final ArtifactStore store : bannedSet )
            {
                assertThat( store.getKey() + " should NOT be present in:\n  " + join( keys( stores ), "\n  " ),
                            stores.contains( store ),
                            equalTo( false ) );
            }
        }
    }

    protected List<StoreKey> keys( final List<? extends ArtifactStore> stores )
    {
        final List<StoreKey> keys = new ArrayList<>();
        for ( final ArtifactStore store : stores )
        {
            keys.add( store.getKey() );
        }

        return keys;
    }

}
