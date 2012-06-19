package org.commonjava.aprox.flat.data;


import org.commonjava.aprox.core.data.RepositoryDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.junit.Rule;

public class FlatRepositoryManagementTest
    extends RepositoryDataManagerTCK
{

    @Rule
    public FlatTCKFixtureProvider provider = new FlatTCKFixtureProvider();

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

}
