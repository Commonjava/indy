package org.commonjava.aprox.subsys.infinispan.inject.fixture;

public class TestValue
{
    public TestValue( final String value )
    {
        this.value = value;
    }

    @SuppressWarnings( "unused" )
    private final String value;
}