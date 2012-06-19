package org.commonjava.aprox.flat.data;


import org.commonjava.aprox.core.data.GroupDataManagerTCK;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.junit.Rule;

public class FlatGroupManagementTest
    extends GroupDataManagerTCK
{

    @Rule
    public FlatTCKFixtureProvider provider = new FlatTCKFixtureProvider();

    @Override
    protected TCKFixtureProvider getFixtureProvider()
    {
        return provider;
    }

}
