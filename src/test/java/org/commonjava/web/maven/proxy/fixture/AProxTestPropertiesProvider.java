package org.commonjava.web.maven.proxy.fixture;

import java.util.Properties;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.commonjava.web.test.fixture.TestPropertyDefinitions;

public class AProxTestPropertiesProvider
{

    @Produces
    @Named( TestPropertyDefinitions.NAMED )
    public Properties getTestProperties()
    {
        Properties props = new Properties();

        props.put( TestPropertyDefinitions.DATABASE_URL,
                   "http://developer.commonjava.org/db/test-aprox" );

        return props;
    }

}
