package org.commonjava.indy.httprox.handler;

import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.http.util.UserPass;
import org.commonjava.indy.util.UrlInfo;
import org.slf4j.Logger;

import static org.commonjava.indy.httprox.util.HttpProxyConstants.PROXY_REPO_PREFIX;
import static org.commonjava.indy.model.core.ArtifactStore.METADATA_ORIGIN;
import static org.commonjava.indy.model.core.ArtifactStore.TRACKING_ID;
import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.indy.model.core.PathStyle.hashed;

/**
 * Created by ruhan on 4/16/18.
 */
public abstract class AbstractProxyRepositoryCreator
                implements ProxyRepositoryCreator
{
    @Override
    public abstract ProxyCreationResult create( String trackingID, String name, String baseUrl, UrlInfo urlInfo,
                                                UserPass userPass, Logger logger );

    @Override
    public String formatId( String host, int port, int index, String trackingID, StoreType storeType )
    {
        host = host.replaceAll( "\\.", "-" );
        if ( trackingID == null )
        {
            return PROXY_REPO_PREFIX + host + "_" + port + ( index > 0 ? "_" + index : "" );
        }
        else
        {
            String prefix = null;
            switch ( storeType )
            {
                case group:
                    prefix = "g";
                    break;
                case remote:
                    prefix = "r";
                    break;
                case hosted:
                    prefix = "h";
                    break;
                default:
                    break;
            }
            return prefix + "-" + host + "-" + trackingID;
        }
    }

    protected RemoteRepository createRemote( String name, String baseUrl, UrlInfo info, UserPass up, Logger logger )
    {
        return createRemote( null, name, baseUrl, info, up, logger );
    }

    protected RemoteRepository createRemote( String trackingID, String name, String baseUrl, UrlInfo info, UserPass up,
                                             Logger logger )
    {
        RemoteRepository remote = new RemoteRepository( GENERIC_PKG_KEY, name, baseUrl );
        if ( up != null )
        {
            remote.setUser( up.getUser() );
            remote.setPassword( up.getPassword() );
        }
        if ( trackingID == null )
        {
            remote.setPassthrough( true ); // to prevent long-term caching of content, default false
        }
        setPropsAndMetadata( remote, trackingID, info );
        return remote;
    }

    protected HostedRepository createHosted( String trackingID, String name, UrlInfo info, Logger logger )
    {
        HostedRepository hosted = new HostedRepository( GENERIC_PKG_KEY, name );
        setPropsAndMetadata( hosted, trackingID, info );
        return hosted;
    }

    protected Group createGroup( String trackingID, String name, UrlInfo info, Logger logger, StoreKey... constituents )
    {
        Group group = new Group( GENERIC_PKG_KEY, name, constituents );
        setPropsAndMetadata( group, trackingID, info );
        return group;
    }

    private void setPropsAndMetadata( ArtifactStore store, String trackingID, UrlInfo info )
    {
        store.setDescription( "HTTProx proxy based on: " + info.getUrl() );
        store.setPathStyle( hashed );

        store.setMetadata( METADATA_ORIGIN, ProxyAcceptHandler.HTTPROX_ORIGIN );
        if ( trackingID != null )
        {
            store.setMetadata( TRACKING_ID, trackingID );
        }
    }
}
