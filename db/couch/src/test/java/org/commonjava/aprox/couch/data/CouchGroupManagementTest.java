package org.commonjava.aprox.couch.data;

import org.commonjava.aprox.core.data.GroupDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.junit.Rule;

public class CouchGroupManagementTest
    extends GroupDataManagerTCK
{

    @Rule
    public CouchTCKFixtureProvider provider = new CouchTCKFixtureProvider();

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

}
