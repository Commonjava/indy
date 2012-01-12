package org.commonjava.aprox.couch.data;

import org.commonjava.aprox.core.data.RepositoryDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.junit.Rule;

public class CouchRepositoryManagementTest
    extends RepositoryDataManagerTCK
{

    @Rule
    public CouchTCKFixtureProvider provider = new CouchTCKFixtureProvider();

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

}
