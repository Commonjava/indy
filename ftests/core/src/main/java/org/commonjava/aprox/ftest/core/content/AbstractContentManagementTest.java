package org.commonjava.aprox.ftest.core.content;

import static org.commonjava.aprox.model.core.StoreType.group;
import static org.commonjava.aprox.model.core.StoreType.remote;

import java.util.Arrays;

import org.commonjava.aprox.ftest.core.AbstractAproxFunctionalTest;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.aprox.model.core.HostedRepository;
import org.commonjava.aprox.model.core.RemoteRepository;
import org.junit.Before;

public class AbstractContentManagementTest
    extends AbstractAproxFunctionalTest
{

    protected static final String STORE = "test";

    protected static final String CENTRAL = "central";

    protected static final String PUBLIC = "public";

    @Before
    public void before()
        throws Exception
    {
        final String changelog = "Create test structures";

        final HostedRepository hosted =
            this.client.stores()
                       .create( new HostedRepository( STORE ), changelog, HostedRepository.class );

        RemoteRepository central = null;
        if ( !client.stores()
                    .exists( remote, CENTRAL ) )
        {
            central =
                client.stores()
                      .create( new RemoteRepository( CENTRAL, "http://repo.maven.apache.org/maven2/" ), changelog,
                               RemoteRepository.class );
        }

        Group g;
        if ( client.stores()
                   .exists( group, PUBLIC ) )
        {
            g = client.stores()
                      .load( group, PUBLIC, Group.class );
        }
        else
        {
            g = client.stores()
                      .create( new Group( PUBLIC ), changelog, Group.class );
        }

        g.setConstituents( Arrays.asList( hosted.getKey(), central.getKey() ) );
        client.stores()
              .update( g, changelog );
    }

}
