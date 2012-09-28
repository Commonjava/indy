package org.commonjava.aprox.subsys.infinispan.inject.fixture;

public final class TestKey
{
    public TestKey( final String key )
    {
        this.key = key;
    }

    @SuppressWarnings( "unused" )
    private final String key;
}