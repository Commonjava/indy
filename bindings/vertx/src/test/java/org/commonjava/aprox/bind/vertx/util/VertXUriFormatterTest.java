package org.commonjava.aprox.bind.vertx.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.StoreType;
import org.junit.Test;

public class VertXUriFormatterTest
{

    @Test
    public void absoluteURIToStorePath()
    {
        final String path = "org/commonjava/aprox/aprox-api/0.9/aprox-api-0.9.pom";
        final String storeName = "test-repo";
        final StoreType type = StoreType.repository;

        final String uri = new VertXUriFormatter().formatAbsolutePathTo( type.singularEndpointName(), storeName, path );
        assertThat( uri, equalTo( "/api/1.0/" + type.singularEndpointName() + "/" + storeName + "/" + path ) );
    }

}
