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
package org.commonjava.indy.httprox.util;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpRequest;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.core.ctl.ContentController;
import org.commonjava.indy.data.ArtifactStoreQuery;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.folo.ctl.FoloConstants;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.httprox.conf.HttproxConfig;
import org.commonjava.indy.httprox.handler.AbstractProxyRepositoryCreator;
import org.commonjava.indy.httprox.handler.ProxyCreationResult;
import org.commonjava.indy.httprox.handler.ProxyRepositoryCreator;
import org.commonjava.indy.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.model.core.AccessChannel;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.subsys.http.util.UserPass;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.indy.util.UrlInfo;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static java.lang.Integer.parseInt;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.CONTENT_ENTRY_POINT;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.METADATA_CONTENT;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.PACKAGE_TYPE;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.PATH;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.getContext;
import static org.commonjava.indy.bind.jaxrs.RequestContextHelper.setContext;
import static org.commonjava.indy.model.core.ArtifactStore.TRACKING_ID;
import static org.commonjava.indy.model.core.GenericPackageTypeDescriptor.GENERIC_PKG_KEY;
import static org.commonjava.maven.galley.io.SpecialPathConstants.PKG_TYPE_GENERIC_HTTP;

/**
 * Created by ruhan on 9/20/18.
 */
public class ProxyResponseHelper
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private static final String TRACKED_USER_SUFFIX = "+tracking";

    private final HttpRequest httpRequest;

    private final ProxyRepositoryCreator repoCreator;

    private final StoreDataManager storeManager;

    private final IndyMetricsConfig metricsConfig;

    private final MetricRegistry metricRegistry;

    private final String cls;

    private final HttproxConfig config;

    private final ContentController contentController;

    private boolean transferred;

    public ProxyResponseHelper( HttpRequest httpRequest, HttproxConfig config, ContentController contentController,
                                ProxyRepositoryCreator repoCreator, StoreDataManager storeManager, IndyMetricsConfig metricsConfig, MetricRegistry metricRegistry, String cls )
    {
        this.httpRequest = httpRequest;
        this.config = config;
        this.contentController = contentController;
        this.repoCreator = repoCreator;
        this.storeManager = storeManager;
        this.metricsConfig = metricsConfig;
        this.metricRegistry = metricRegistry;
        this.cls = cls;
    }

    public ArtifactStore getArtifactStore( String trackingId, final URL url )
                    throws IndyDataException
    {
        ArtifactStore store = null;
        if ( metricsConfig == null || metricRegistry == null )
        {
            store = doGetArtifactStore( trackingId, url );
        }

        if ( store == null )
        {
            Timer timer = metricRegistry.timer( name( metricsConfig.getNodePrefix(), cls, "getArtifactStore" ) );
            Timer.Context timerContext = timer.time();
            try
            {
                store = doGetArtifactStore( trackingId, url );
            }
            finally
            {
                timerContext.stop();
            }
        }

        setContext( PACKAGE_TYPE, store.getKey().getPackageType() );
        setContext( CONTENT_ENTRY_POINT, store.getKey().toString() );

        return store;
    }

    private ArtifactStore doGetArtifactStore( String trackingId, final URL url )
                    throws IndyDataException
    {
        int port = getPort( url );

        if ( trackingId != null )
        {
            String groupName = repoCreator.formatId( url.getHost(), port, 0, trackingId, StoreType.group );

            ArtifactStoreQuery<Group> query =
                            storeManager.query().packageType( GENERIC_PKG_KEY ).storeType( Group.class );

            Group group = query.getGroup( groupName );
            logger.debug( "Get httproxy group, group: {}", group );

            if ( group == null )
            {
                logger.debug( "Creating repositories (group, hosted, remote) for HTTProx request: {}, trackingId: {}",
                              url, trackingId );
                ProxyCreationResult result = createRepo( trackingId, url, null );
                group = result.getGroup();
            }
            return group;
        }
        else
        {
            RemoteRepository remote;
            final String baseUrl = getBaseUrl( url, false );

            ArtifactStoreQuery<RemoteRepository> query =
                            storeManager.query().packageType( GENERIC_PKG_KEY ).storeType( RemoteRepository.class );

            remote = query.stream()
                          .filter( store -> store.getUrl().equals( baseUrl )
                                          && store.getMetadata( TRACKING_ID ) == null )
                          .findFirst()
                          .orElse( null );

            logger.debug( "Get httproxy remote, remote: {}", remote );
            if ( remote == null )
            {
                logger.debug( "Creating remote repository for HTTProx request: {}", url );
                String name = getRemoteRepositoryName( url );
                ProxyCreationResult result = createRepo( null, url, name );
                remote = result.getRemote();
            }
            return remote;
        }
    }

    /**
     * Create repositories (group, remote, hosted) when trackingId is present. Otherwise create normal remote
     * repository with specified name.
     *
     * @param trackingId
     * @param url
     * @param name distinct remote repository name. null if trackingId is given
     */
    private ProxyCreationResult createRepo( String trackingId, URL url, String name )
                    throws IndyDataException
    {
        UrlInfo info = new UrlInfo( url.toExternalForm() );

        UserPass up = UserPass.parse( ApplicationHeader.authorization, httpRequest, url.getAuthority() );
        String baseUrl = getBaseUrl( url, false );

        logger.debug( ">>>> Create repo: trackingId=" + trackingId + ", name=" + name );
        ProxyCreationResult result = repoCreator.create( trackingId, name, baseUrl, info, up,
                                                         LoggerFactory.getLogger( repoCreator.getClass() ) );
        ChangeSummary changeSummary =
                        new ChangeSummary( ChangeSummary.SYSTEM_USER, "Creating HTTProx proxy for: " + info.getUrl() );

        RemoteRepository remote = result.getRemote();
        if ( remote != null )
        {
            storeManager.storeArtifactStore( remote, changeSummary, false, true, new EventMetadata() );
        }

        HostedRepository hosted = result.getHosted();
        if ( hosted != null )
        {
            storeManager.storeArtifactStore( hosted, changeSummary, false, true, new EventMetadata() );
        }

        Group group = result.getGroup();
        if ( group != null )
        {
            storeManager.storeArtifactStore( group, changeSummary, false, true, new EventMetadata() );
        }

        return result;
    }

    /**
     * if repo with this name already exists, we need to use a different name
     */
    private String getRemoteRepositoryName( URL url ) throws IndyDataException
    {
        final String name = repoCreator.formatId( url.getHost(), getPort( url ), 0, null, StoreType.remote );

        logger.debug( "Looking for remote repo starts with name: {}", name );

        AbstractProxyRepositoryCreator abstractProxyRepositoryCreator = null;
        if ( repoCreator instanceof AbstractProxyRepositoryCreator )
        {
            abstractProxyRepositoryCreator = (AbstractProxyRepositoryCreator) repoCreator;
        }

        if ( abstractProxyRepositoryCreator == null )
        {
            return name;
        }

        Predicate<ArtifactStore> filter = abstractProxyRepositoryCreator.getNameFilter( name );
        List<String> l = storeManager.query()
                                     .packageType( GENERIC_PKG_KEY )
                                     .storeType( RemoteRepository.class )
                                     .stream( filter )
                                     .map( repository -> repository.getName() )
                                     .collect( Collectors.toList() );

        if ( l.isEmpty() )
        {
            return name;
        }
        return abstractProxyRepositoryCreator.getNextName( l );
    }

    private int getPort( URL url )
    {
        int port = url.getPort();
        if ( port < 1 )
        {
            port = url.getDefaultPort();
        }
        return port;
    }

    private String getBaseUrl( URL url, boolean includeDefaultPort )
    {
        int port = getPort( url );
        String portStr;
        if ( includeDefaultPort || port != url.getDefaultPort() )
        {
            portStr = ":" + port;
        }
        else
        {
            portStr = "";
        }
        return String.format( "%s://%s%s/", url.getProtocol(), url.getHost(), portStr );
    }

    public void transfer( final HttpConduitWrapper http, final ArtifactStore store, final String path,
                   final boolean writeBody, final UserPass proxyUserPass, final ProxyMeter meter )
                    throws IOException, IndyWorkflowException
    {
        setContext( PATH, path );
        setContext( METADATA_CONTENT, Boolean.toString( false ) );

        if ( metricsConfig == null || metricRegistry == null )
        {
            doTransfer( http, store, path, writeBody, proxyUserPass, meter );
            return;
        }

        Timer timer = metricRegistry.timer( name( metricsConfig.getNodePrefix(), cls, "transfer" ) );
        Timer.Context timerContext = timer.time();
        try
        {
            doTransfer( http, store, path, writeBody, proxyUserPass, meter );
        }
        finally
        {
            timerContext.stop();
        }
    }

    private void doTransfer( final HttpConduitWrapper http, final ArtifactStore store, final String path,
                             final boolean writeBody, final UserPass proxyUserPass, final ProxyMeter meter )
                    throws IOException, IndyWorkflowException
    {
        if ( transferred )
        {
            return;
        }

        transferred = true;
        if ( !http.isOpen() )
        {
            throw new IOException( "Sink channel already closed (or null)!" );
        }

        final EventMetadata eventMetadata = createEventMetadata( writeBody, proxyUserPass, path, store );

        Transfer txfr = null;
        try
        {
            txfr = contentController.get( store.getKey(), path, eventMetadata );
        }
        catch ( final IndyWorkflowException e )
        {
            if ( !( e.getCause() instanceof TransferException ) )
            {
                throw e;
            }
            logger.debug( "Suppressed exception for further handling inside proxy logic:", e );
        }

        if ( txfr != null && txfr.exists() )
        {
            meter.reportResponseSummary();
            http.writeExistingTransfer( txfr, writeBody, path, eventMetadata );
        }
        else
        {
            http.writeNotFoundTransfer( store, path );
        }
    }

    private EventMetadata createEventMetadata( final boolean writeBody, final UserPass proxyUserPass, final String path,
                                       final ArtifactStore store )
                    throws IndyWorkflowException
    {
        final EventMetadata eventMetadata = new EventMetadata();
        if ( writeBody )
        {
            TrackingKey tk = getTrackingKey( proxyUserPass );

            if ( tk != null )
            {
                logger.debug( "TRACKING {} in {} (KEY: {})", path, store, tk );
                eventMetadata.set( FoloConstants.TRACKING_KEY, tk );

                eventMetadata.set( FoloConstants.ACCESS_CHANNEL, AccessChannel.GENERIC_PROXY );
            }
            else
            {
                logger.debug( "NOT TRACKING: {} in {}", path, store );
            }
        }
        else
        {
            logger.debug( "NOT TRACKING non-body request: {} in {}", path, store );
        }

        eventMetadata.setPackageType( PKG_TYPE_GENERIC_HTTP );

        return eventMetadata;
    }

    public TrackingKey getTrackingKey( UserPass proxyUserPass ) throws IndyWorkflowException
    {
        TrackingKey tk = null;
        switch ( config.getTrackingType() )
        {
            case ALWAYS:
            {
                if ( proxyUserPass == null )
                {
                    throw new IndyWorkflowException( ApplicationStatus.BAD_REQUEST.code(),
                                                     "Tracking is always-on, but no username was provided! Cannot initialize tracking key." );
                }

                tk = new TrackingKey( proxyUserPass.getUser() );

                break;
            }
            case SUFFIX:
            {
                if ( proxyUserPass != null )
                {
                    final String user = proxyUserPass.getUser();

                    if ( user != null && user.endsWith( TRACKED_USER_SUFFIX ) && user.length() > TRACKED_USER_SUFFIX.length() )
                    {
                        tk = new TrackingKey( StringUtils.substring( user, 0, - TRACKED_USER_SUFFIX.length() ) );
                    }
                }

                break;
            }
            default:
            {
            }
        }
        return tk;
    }

}
