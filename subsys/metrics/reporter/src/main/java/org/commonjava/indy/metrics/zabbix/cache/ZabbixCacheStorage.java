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
package org.commonjava.indy.metrics.zabbix.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xiabai on 5/8/17.
 */

@ApplicationScoped
public class ZabbixCacheStorage
                implements StartupAction
{
    private static final Logger logger = LoggerFactory.getLogger( ZabbixCacheStorage.class );

    private final static String ZABBIX_ID = "zabbix";

    private final static String ZABBIX_HOSTGROUP = "hostGroupCache.json";

    private final static String ZABBIX_HOST = "hostCache.json";

    private final static String ZABBIX_ITEM = "itemCache.json";

    private DataFile hostGroupCacheDataFile = null;

    private DataFile hostCacheDataFile = null;

    private DataFile itemCacheDataFile = null;

    // name, hostGroupId
    private ConcurrentHashMap<String, String> hostGroupCache = new ConcurrentHashMap<String, String>();

    // name, hostId
    private ConcurrentHashMap<String, String> hostCache = new ConcurrentHashMap<String, String>();

    // name, itemId
    private ConcurrentHashMap<String, String> itemCache = new ConcurrentHashMap<String, String>();

    @Inject
    private DataFileManager dataFileManager;

    @Override
    public int getStartupPriority()
    {
        return 30;
    }

    @Override
    public String getId()
    {
        return ZABBIX_ID;
    }

    @Override
    public void start() throws IndyLifecycleException
    {
        this.reLoadHostGroup();
        this.reLoadHost();
        this.reLoadItem();
    }

    private void reLoadHostGroup()
    {
        hostGroupCacheDataFile = dataFileManager.getDataFile( ZABBIX_ID + "/" + ZABBIX_HOSTGROUP );

        ObjectMapper mapper = new ObjectMapper();
        if ( hostGroupCacheDataFile.exists() )
        {
            try
            {
                HashMap m  = new HashMap(  );
                m.put( "test","123" );
                mapper.writeValueAsString( m );
                String hostGroups = hostGroupCacheDataFile.readString();
                hostGroupCache = mapper.readValue( hostGroups, ConcurrentHashMap.class );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                logger.error( "read hostGroup cache from data file have a error:" + e );
            }
        }
    }

    private void reLoadHost()
    {
        hostCacheDataFile = dataFileManager.getDataFile( ZABBIX_ID + "/" + ZABBIX_HOST );

        ObjectMapper mapper = new ObjectMapper();
        if ( hostCacheDataFile.exists() )
        {
            try
            {
                String hosts = hostCacheDataFile.readString();
                hostCache = mapper.readValue( hosts, ConcurrentHashMap.class );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                logger.error( "read hosts cache from data file have a error:" + e );
            }
        }
    }

    private void reLoadItem()
    {
        itemCacheDataFile = dataFileManager.getDataFile( ZABBIX_ID + "/" + ZABBIX_ITEM );
        ObjectMapper mapper = new ObjectMapper();

        if ( itemCacheDataFile.exists() )
        {
            try
            {
                String items = itemCacheDataFile.readString();
                itemCache = mapper.readValue( items, ConcurrentHashMap.class );
            }
            catch ( IOException e )
            {
                e.printStackTrace();
                logger.error( "read items cache from data file have a error:" + e );
            }
        }
    }

    public void putHostGroup( String name, String id ) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        hostGroupCache.put( name, id );
        hostGroupCacheDataFile.writeString( mapper.writeValueAsString( hostGroupCache ), "UTF-8", null );
    }

    public String getHostGroup( String name )
    {
        return hostGroupCache.get( name );
    }

    public void putHost( String name, String id ) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        hostCache.put( name, id );
        hostCacheDataFile.writeString( mapper.writeValueAsString( hostCache ), "UTF-8", null );
    }

    public String getHost( String name )
    {
        return hostCache.get( name );
    }

    public void putItem( String name, String id ) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        itemCache.put( name, id );
        itemCacheDataFile.writeString( mapper.writeValueAsString( itemCache ), "UTF-8", null );
    }

    public String getItem( String name )
    {
        return itemCache.get( name );
    }
}
