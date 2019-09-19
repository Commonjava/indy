/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.ftest.metrics.jaxrs;

import org.commonjava.indy.bind.jaxrs.IndyResources;
import org.commonjava.indy.metrics.zabbix.cache.ZabbixCacheStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

/**
 * Created by xiabai on 5/8/17.
 */
@Path( "/api/ftest/metrics/" )
@Produces( "application/json" )
@Consumes( "application/json" )
public class ZabbixCacheStorageResource
                implements IndyResources
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    ZabbixCacheStorage zabbixCacheStorage;

    @GET
    @Path( "/storage/put" )
    public Response putCache() throws IOException
    {
        zabbixCacheStorage.putHost( "test-host","123" );
        zabbixCacheStorage.putHostGroup( "test-host-group","456" );
        zabbixCacheStorage.putItem( "test-item","789" );
        return Response.ok("\"well done\"").build();
    }

    @GET
    @Path( "/storage/putHostGroup" )
    public Response getHostGroupCache() throws IOException
    {
        String result = zabbixCacheStorage.getHostGroup( "test-host-group" );
        return Response.ok( result, MediaType.APPLICATION_JSON ).build();
    }

    @GET
    @Path( "/storage/putHost" )
    public Response getHostCache() throws IOException
    {
        String result = zabbixCacheStorage.getHost( "test-host" );
        return Response.ok( result, MediaType.APPLICATION_JSON ).build();
    }

    @GET
    @Path( "/storage/putItem" )
    public Response getItemCache() throws IOException
    {
        String result = zabbixCacheStorage.getItem( "test-item" );
        return Response.ok( result, MediaType.APPLICATION_JSON ).build();
    }

}
