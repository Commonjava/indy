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
package org.commonjava.indy.metrics.zabbix.sender;

import org.commonjava.indy.IndyException;
import org.commonjava.indy.metrics.exception.IndyMetricsException;
import org.commonjava.indy.metrics.zabbix.api.IndyZabbixApi;
import org.commonjava.indy.metrics.zabbix.api.ZabbixApi;
import org.commonjava.indy.metrics.zabbix.cache.ZabbixCacheStorage;
import org.commonjava.indy.subsys.http.IndyHttpException;
import org.commonjava.indy.subsys.http.IndyHttpProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by xiabai on 4/1/17.
 */
public class IndyZabbixSender
{
    private static final Logger logger = LoggerFactory.getLogger( IndyZabbixSender.class );

    private ZabbixSender sender;

    private boolean bCreateNotExistHostGroup = true;

    private boolean bCreateNotExistHost = true;

    private boolean bCreateNotExistItem = true;

    private boolean bCreateNotExistZabbixApi = true;

    private ZabbixApi zabbixApi;

    private String zabbixHostUrl;

    private String hostGroup = "NOS";//// default host group

    private String group = "NOS";

    private long clock = 0l;

    private String hostName;

    private String ip;

    private String zabbixUserName;

    private String zabbixUserPwd;

    private ZabbixCacheStorage zabbixCacheStorage;

    private IndyHttpProvider indyHttpProvider;

    private static final String regEx = "^-?[0-9]+$";

    private static final Pattern pat = Pattern.compile( regEx );

    public static Builder create()
    {
        return new Builder();
    }

    public static class Builder
    {
        boolean bCreateNotExistHostGroup = true;

        boolean bCreateNotExistHost = true;

        boolean bCreateNotExistItem = true;

        boolean bCreateNotExistZabbixSender = true;

        ZabbixApi zabbixApi;

        String zabbixHostUrl;

        String hostGroup = "NOS";//// default host group

        String group = "NOS";

        long clock = 0l;

        String hostName;

        String ip;

        String zabbixUserName;

        String zabbixUserPwd;

        String zabbixHost;

        int zabbixPort;

        ZabbixCacheStorage zabbixCacheStorage;

        IndyHttpProvider indyHttpProvider;

        public Builder metricsZabbixCache( ZabbixCacheStorage zabbixCacheStorage )
        {
            this.zabbixCacheStorage = zabbixCacheStorage;
            return this;
        }

        public Builder indyHttpProvider( IndyHttpProvider indyHttpProvider )
        {
            this.indyHttpProvider = indyHttpProvider;
            return this;
        }

        public Builder zabbixHost( String zabbixHost )
        {
            this.zabbixHost = zabbixHost;
            return this;
        }

        public Builder hostName( String hostName )
        {
            this.hostName = hostName;
            return this;
        }

        public Builder zabbixPort( int zabbixPort )
        {
            this.zabbixPort = zabbixPort;
            return this;
        }

        public Builder bCreateNotExistHostGroup( boolean bCreateNotExistHostGroup )
        {
            this.bCreateNotExistHostGroup = bCreateNotExistHostGroup;
            return this;
        }

        public Builder bCreateNotExistHost( boolean bCreateNotExistHost )
        {
            this.bCreateNotExistHost = bCreateNotExistHost;
            return this;
        }

        public Builder bCreateNotExistItem( boolean bCreateNotExistItem )
        {
            this.bCreateNotExistItem = bCreateNotExistItem;
            return this;
        }

        public Builder bCreateNotExistZabbixSender( boolean bCreateNotExistZabbixSender )
        {
            this.bCreateNotExistZabbixSender = bCreateNotExistZabbixSender;
            return this;
        }

        public Builder zabbixApi( ZabbixApi zabbixApi )
        {
            this.zabbixApi = zabbixApi;
            return this;
        }

        public Builder zabbixHostUrl( String zabbixHostUrl )
        {
            this.zabbixHostUrl = zabbixHostUrl;
            return this;
        }

        public Builder hostGroup( String hostGroup )
        {
            this.hostGroup = hostGroup;
            return this;
        }

        public Builder group( String group )
        {
            this.group = group;
            return this;
        }

        public Builder ip( String ip )
        {
            this.ip = ip;
            return this;
        }

        public Builder zabbixUserName( String zabbixUserName )
        {
            this.zabbixUserName = zabbixUserName;
            return this;
        }

        public Builder clock( long clock )
        {
            this.clock = clock;
            return this;
        }

        public Builder zabbixUserPwd( String zabbixUserPwd )
        {
            this.zabbixUserPwd = zabbixUserPwd;
            return this;
        }

        public IndyZabbixSender build()
        {
            return new IndyZabbixSender( this.bCreateNotExistHostGroup, this.bCreateNotExistHost,
                                         this.bCreateNotExistItem, this.bCreateNotExistZabbixSender, this.zabbixApi,
                                         this.zabbixHostUrl, this.hostGroup, this.group, this.clock, this.hostName,
                                         this.ip, this.zabbixUserName, this.zabbixUserPwd, this.zabbixHost,
                                         this.zabbixPort, this.indyHttpProvider, this.zabbixCacheStorage );
        }

    }

    public IndyZabbixSender( boolean bCreateNotExistHostGroup, boolean bCreateNotExistHost, boolean bCreateNotExistItem,
                             boolean bCreateNotExistZabbixApi, ZabbixApi zabbixApi, String zabbixHostUrl,
                             String hostGroup, String group, long clock, String hostName, String ip,
                             String zabbixUserName, String zabbixUserPwd, String zabbixHost, int zabbixPort,
                             IndyHttpProvider indyHttpProvider, ZabbixCacheStorage zabbixCacheStorage )
    {

        this.bCreateNotExistHostGroup = bCreateNotExistHostGroup;
        this.bCreateNotExistHost = bCreateNotExistHost;
        this.bCreateNotExistItem = bCreateNotExistItem;
        this.bCreateNotExistZabbixApi = bCreateNotExistZabbixApi;
        this.zabbixApi = zabbixApi;
        this.zabbixHostUrl = zabbixHostUrl;
        this.hostGroup = hostGroup;
        this.group = group;
        this.clock = clock;
        this.hostName = hostName;
        this.ip = ip;
        this.zabbixUserName = zabbixUserName;
        this.zabbixUserPwd = zabbixUserPwd;
        this.indyHttpProvider = indyHttpProvider;
        this.zabbixCacheStorage = zabbixCacheStorage;
        this.sender = new ZabbixSender( zabbixHost, zabbixPort );
    }

    String checkHostGroup( String hostGroup ) throws IOException, IndyHttpException, IndyMetricsException
    {
        if ( zabbixCacheStorage.getHostGroup( hostGroup ) == null )
        {
            try
            {
                this.zabbixApiInit();
                String groupid = zabbixApi.getHostgroup( hostGroup );
                if ( groupid == null )
                {
                    groupid = zabbixApi.hostgroupCreate( hostGroup );
                    zabbixCacheStorage.putHostGroup( hostGroup, groupid );
                }
                zabbixCacheStorage.putHostGroup( hostGroup, groupid );
                return groupid;
            }
            finally
            {
                this.destroy();
            }
        }
        return null;
    }

    String checkHost( String host, String ip ) throws IOException, IndyHttpException, IndyMetricsException
    {
        try
        {
            if ( zabbixCacheStorage.getHost( host ) == null )
            {
                this.zabbixApiInit();
                String hostid = zabbixApi.getHost( host );
                if ( hostid != null )
                {
                    zabbixCacheStorage.putHost( host, hostid );

                }
                else
                {// host not exists, create it.

                    hostid = zabbixApi.hostCreate( host, zabbixCacheStorage.getHostGroup( hostGroup ), ip );
                    zabbixCacheStorage.putHost( host, hostid );
                }
                return hostid;
            }
        }
        finally
        {
            this.destroy();
        }
        return null;
    }

    private String itemCacheKey( String host, String item )
    {
        return host + ":" + item;
    }

    String checkItem( String host, String item, int valueType ) throws IOException, IndyHttpException, IndyMetricsException
    {

        try
        {
            if ( zabbixCacheStorage.getItem( itemCacheKey( host, item ) ) == null )
            {
                this.zabbixApiInit();

                String itemid = zabbixApi.getItem( host, item, zabbixCacheStorage.getHost( host ) );
                if ( itemid == null )
                {
                    itemid = zabbixApi.createItem( host, item, zabbixCacheStorage.getHost( host ), valueType );
                    zabbixCacheStorage.putItem( itemCacheKey( host, item ), itemid );
                }
                else
                {
                    // put into metricsZabbixCache
                    zabbixCacheStorage.putItem( itemCacheKey( host, item ), itemid );
                }
                return itemid;
            }
        }
        finally
        {
            this.destroy();
        }

        return null;
    }

    public SenderResult send( DataObject dataObject ) throws IOException, IndyException
    {
        return this.send( dataObject, System.currentTimeMillis() / 1000L );
    }

    public SenderResult send( DataObject dataObject, long clock ) throws IOException, IndyException
    {
        return this.send( Collections.singletonList( dataObject ), clock );
    }

    public SenderResult send( List<DataObject> dataObjectList ) throws IOException, IndyException
    {
        return this.send( dataObjectList, System.currentTimeMillis() / 1000L );
    }

    /**
     *
     * @param dataObjectList
     * @param clock
     *            TimeUnit is SECONDS.
     * @return
     * @throws IOException
     */
    public SenderResult send( List<DataObject> dataObjectList, long clock )
                    throws IOException, IndyHttpException, IndyMetricsException
    {
        if ( bCreateNotExistHostGroup )
        {
            try
            {
                checkHostGroup( hostGroup );
            }
            catch ( IndyHttpException e )
            {
                logger.error( "Check HostGroup of Zabbix is error:" + e.getMessage() );
                throw e;
            }
        }
        if ( bCreateNotExistHost )
        {
            try
            {
                checkHost( hostName, ip );
            }
            catch ( IndyHttpException e )
            {
                logger.error( "Check Host of Zabbix is error:" + e.getMessage() );
                throw e;
            }
        }

        if ( bCreateNotExistItem )
        {
            for ( DataObject object : dataObjectList )
            {
                String key = object.getKey();
                int vauleType = 0;
                Matcher mat = pat.matcher( object.getValue() );
                if ( !mat.find() )
                {
                    vauleType = 4;
                }
                try
                {
                    checkItem( hostName, key, vauleType );
                }
                catch ( IndyHttpException e )
                {
                    logger.error( "Check Item of Zabbix is error:" + e.getMessage() );
                    throw e;
                }
            }
        }

        try
        {
            SenderResult senderResult = sender.send( dataObjectList, clock );
            if ( !senderResult.success() )
            {
                logger.error( "send data to zabbix server error! senderResult:" + senderResult );
            }
            return senderResult;
        }
        catch ( IOException e )
        {
            logger.error( "send data to zabbix server error!", e );
            throw e;
        }
    }

    public void destroy()
    {
        if ( bCreateNotExistZabbixApi )
        {
            return;
        }
        if ( zabbixApi != null )
            zabbixApi.destroy();
    }

    private void zabbixApiInit() throws IndyMetricsException, IOException, IndyHttpException
    {
        if ( !bCreateNotExistZabbixApi )
        {
            return;
        }
        if ( this.zabbixHostUrl == null || "".equals( this.zabbixHostUrl ) )
        {
            throw new IndyMetricsException( "can not find Zabbix's Host" );
        }

        zabbixApi = new IndyZabbixApi( this.zabbixHostUrl, indyHttpProvider.createClient( new URL( zabbixHostUrl ).getHost() ) );

        zabbixApi.init();

        if ( this.zabbixUserName == null || "".equals( this.zabbixUserName ) || this.zabbixUserPwd == null || "".equals(
                        this.zabbixUserPwd ) )
        {
            throw new IndyMetricsException( "can not find Zabbix's username or password" );
        }
        boolean login = zabbixApi.login( this.zabbixUserName, this.zabbixUserPwd );

        logger.info( "User:" + this.zabbixUserName + " login is " + login );
    }

    public ZabbixApi getZabbixApi()
    {
        return zabbixApi;
    }

    public void setZabbixApi( ZabbixApi zabbixApi )
    {
        this.zabbixApi = zabbixApi;
    }

    public boolean isbCreateNotExistHost()
    {
        return bCreateNotExistHost;
    }

    public void setbCreateNotExistHost( boolean bCreateNotExistHost )
    {
        this.bCreateNotExistHost = bCreateNotExistHost;
    }

    public String getHostGroup()
    {
        return hostGroup;
    }

    public void setHostGroup( String hostGroup )
    {
        this.hostGroup = hostGroup;
    }

    public boolean isbCreateNotExistItem()
    {
        return bCreateNotExistItem;
    }

    public void setbCreateNotExistItem( boolean bCreateNotExistItem )
    {
        this.bCreateNotExistItem = bCreateNotExistItem;
    }

    public String getGroup()
    {
        return group;
    }

    public void setGroup( String group )
    {
        this.group = group;
    }

    public long getClock()
    {
        return clock;
    }

    public void setClock( long clock )
    {
        this.clock = clock;
    }

    public String getHostName()
    {
        return hostName;
    }

    public void setHostName( String hostName )
    {
        this.hostName = hostName;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp( String ip )
    {
        this.ip = ip;
    }

    public boolean isbCreateNotExistHostGroup()
    {
        return bCreateNotExistHostGroup;
    }

    public void setbCreateNotExistHostGroup( boolean bCreateNotExistHostGroup )
    {
        this.bCreateNotExistHostGroup = bCreateNotExistHostGroup;
    }

    public String getZabbixHostUrl()
    {
        return zabbixHostUrl;
    }

    public void setZabbixHostUrl( String zabbixHostUrl )
    {
        this.zabbixHostUrl = zabbixHostUrl;
    }

    public String getZabbixUserName()
    {
        return zabbixUserName;
    }

    public void setZabbixUserName( String zabbixUserName )
    {
        this.zabbixUserName = zabbixUserName;
    }

    public String getZabbixUserPwd()
    {
        return zabbixUserPwd;
    }

    public void setZabbixUserPwd( String zabbixUserPwd )
    {
        this.zabbixUserPwd = zabbixUserPwd;
    }

    public boolean isbCreateNotExistZabbixApi()
    {
        return bCreateNotExistZabbixApi;
    }

    public void setbCreateNotExistZabbixApi( boolean bCreateNotExistZabbixApi )
    {
        this.bCreateNotExistZabbixApi = bCreateNotExistZabbixApi;
    }

}
