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
package org.commonjava.indy.core.ctl;

import static org.commonjava.maven.galley.util.UrlUtils.buildUrl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.commonjava.indy.IndyException;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.core.dto.EndpointView;
import org.commonjava.indy.model.core.dto.EndpointViewListing;
import org.commonjava.indy.model.core.dto.ReplicationAction;
import org.commonjava.indy.model.core.dto.ReplicationAction.ActionType;
import org.commonjava.indy.model.core.dto.ReplicationDTO;
import org.commonjava.indy.model.core.dto.StoreListingDTO;
import org.commonjava.indy.subsys.http.IndyHttpException;
import org.commonjava.indy.subsys.http.IndyHttpProvider;
import org.commonjava.indy.subsys.http.util.HttpResources;
import org.commonjava.indy.subsys.template.IndyGroovyException;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.commonjava.maven.galley.event.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

@ApplicationScoped
public class ReplicationController
{
    private static final String REPLICATION_ORIGIN = "replication";

    private static final String REPLICATION_REPO_CREATOR_SCRIPT = "replication-repo-creator.groovy";

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager data;

    @Inject
    private ObjectMapper serializer;

    @Inject
    private IndyHttpProvider http;

    @Inject
    private ScriptEngine scriptEngine;

    protected ReplicationController()
    {
    }

    public ReplicationController( final StoreDataManager data, final IndyHttpProvider http, ScriptEngine scriptEngine,
                                  final ObjectMapper serializer )
    {
        this.data = data;
        this.http = http;
        this.scriptEngine = scriptEngine;
        this.serializer = serializer;
    }

    public ReplicationRepositoryCreator createRepoCreator()
    {
        ReplicationRepositoryCreator creator = null;
        try
        {
            creator = scriptEngine.parseStandardScriptInstance( ScriptEngine.StandardScriptType.store_creators,
                                                                REPLICATION_REPO_CREATOR_SCRIPT,
                                                                ReplicationRepositoryCreator.class );
        }
        catch ( IndyGroovyException e )
        {
            Logger logger = LoggerFactory.getLogger( getClass() );
            logger.error( String.format( "Cannot create ReplicationRepositoryCreator instance: %s. Disabling replication support.",
                                         e.getMessage() ), e );
        }
        return creator;
    }

    public Set<StoreKey> replicate( final ReplicationDTO dto, final String user )
        throws IndyWorkflowException
    {

        final ReplicationRepositoryCreator creator = createRepoCreator();

        if ( creator == null )
        {
            throw new IndyWorkflowException( 500, "Cannot replicate; ReplicationRepositoryCreator could not be instantiated." );
        }

        try
        {
            dto.validate();
        }
        catch ( final IndyException e )
        {
            throw new IndyWorkflowException( "Invalid replication request DTO: %s", e, e.getMessage() );
        }

        List<? extends ArtifactStore> remoteStores = null;
        List<EndpointView> remoteEndpoints = null;

        final boolean overwrite = dto.isOverwrite();
        final Set<StoreKey> replicated = new HashSet<StoreKey>();
        for ( final ReplicationAction action : dto )
        {
            if ( action == null )
            {
                continue;
            }

            logger.info( "Processing replication action:\n\n  {}\n\nin DTO: {}\n\n", action, dto );
            final String include = action.getInclude();
            final String exclude = action.getExclude();

            try
            {
                if ( action.getType() == ActionType.PROXY )
                {
                    if ( remoteEndpoints == null )
                    {
                        remoteEndpoints = getEndpoints( dto );
                    }

                    for ( final EndpointView view : remoteEndpoints )
                    {
                        final String key = "remote-" + view.getType() + "_" + view.getName();
                        if ( ( include == null || key.matches( include ) )
                            && ( exclude == null || !key.matches( exclude ) ) )
                        {
                            final StoreKey sk = new StoreKey( StoreType.remote, key );
                            if ( overwrite || !data.hasArtifactStore( sk ) )
                            {
                                RemoteRepository repo = creator.createRemoteRepository( key, view );
                                repo.setMetadata( ArtifactStore.METADATA_ORIGIN, REPLICATION_ORIGIN );

                                setProxyAttributes( repo, action );

                                data.storeArtifactStore( repo, new ChangeSummary( user,
                                                                                     "REPLICATION: Proxying remote indy repository: "
                                                                                      + view.getResourceUri() ),
                                                         true, true,
                                                         new EventMetadata().set( StoreDataManager.EVENT_ORIGIN,
                                                                                  REPLICATION_ORIGIN ) );
                                replicated.add( repo.getKey() );
                            }
                        }
                    }
                }
                else if ( action.getType() == ActionType.MIRROR )
                {
                    if ( remoteStores == null )
                    {
                        remoteStores = getRemoteStores( dto );
                    }

                    for ( final ArtifactStore store : remoteStores )
                    {
                        final String key = store.getKey()
                                                .toString();
                        if ( ( include == null || key.matches( include ) )
                            && ( exclude == null || !key.matches( exclude ) ) )
                        {
                            if ( overwrite || !data.hasArtifactStore( store.getKey() ) )
                            {
                                if ( store instanceof RemoteRepository )
                                {
                                    setProxyAttributes( ( (RemoteRepository) store ), action );
                                }

                                data.storeArtifactStore( store, new ChangeSummary( user,
                                                                                   "REPLICATION: Mirroring remote indy store: "
                                                                                       + store.getKey() ),
                                                         true, true,
                                                         new EventMetadata().set( StoreDataManager.EVENT_ORIGIN,
                                                                                  REPLICATION_ORIGIN ) );
                                replicated.add( store.getKey() );
                            }
                        }
                    }
                }
            }
            catch ( final IndyDataException e )
            {
                logger.error( e.getMessage(), e );
                throw new IndyWorkflowException( e.getMessage(), e );
            }
        }

        return replicated;

    }

    private void setProxyAttributes( final RemoteRepository repo, final ReplicationAction action )
    {
        if ( action.getProxyHost() != null )
        {
            repo.setProxyHost( action.getProxyHost() );

            if ( action.getProxyPort() > 0 )
            {
                repo.setProxyPort( action.getProxyPort() );
            }

            if ( action.getProxyUser() != null )
            {
                repo.setProxyUser( action.getProxyUser() );
            }

            if ( action.getProxyPass() != null )
            {
                repo.setProxyPassword( action.getProxyPass() );
            }
        }
    }

    // FIXME: Find a better solution to the passed-in generic deserialization problem...erasure is a mother...
    private List<? extends ArtifactStore> getRemoteStores( final ReplicationDTO dto )
        throws IndyWorkflowException
    {
        final String apiUrl = dto.getApiUrl();

        String remotesUrl = null;
        String groupsUrl = null;
        String hostedUrl = null;
        try
        {
            remotesUrl = buildUrl( apiUrl, "/admin/remotes" );
            groupsUrl = buildUrl( apiUrl, "/admin/groups" );
            hostedUrl = buildUrl( apiUrl, "/admin/hosted" );
        }
        catch ( final MalformedURLException e )
        {
            throw new IndyWorkflowException(
                                              "Failed to construct store definition-retrieval URL from api-base: {}. Reason: {}",
                                              e, apiUrl, e.getMessage() );
        }

        //        logger.info( "\n\n\n\n\n[AutoProx] Checking URL: {} from:", new Throwable(), url );
        final List<ArtifactStore> result = new ArrayList<ArtifactStore>();

        addStoresFrom( result, remotesUrl, dto, RemoteRepository.class );
        addStoresFrom( result, groupsUrl, dto, Group.class );
        addStoresFrom( result, hostedUrl, dto, HostedRepository.class );

        return result;
    }

    private <T extends ArtifactStore> void addStoresFrom( final List<ArtifactStore> result, final String remotesUrl,
                                                          final ReplicationDTO dto, final Class<T> type )
        throws IndyWorkflowException
    {
        final HttpGet req = newGet( remotesUrl, dto );
        CloseableHttpClient client = null;
        try
        {
            String siteId = new URL( remotesUrl ).getHost();

            client = http.createClient( siteId );
            CloseableHttpResponse response = client.execute( req, http.createContext( siteId ) );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final String json = HttpResources.entityToString( response );

                final StoreListingDTO<T> listing = serializer.readValue( json, serializer.getTypeFactory()
                                                                                         .constructParametricType(
                                                                                                 StoreListingDTO.class,
                                                                                                 type ) );

                if ( listing != null )
                {
                    result.addAll( listing.getItems() );
                }
            }
            else
            {
                throw new IndyWorkflowException( status, "Request: %s failed: %s", remotesUrl, statusLine );
            }
        }
        catch ( final IOException | IndyHttpException e )
        {
            throw new IndyWorkflowException( "Failed to retrieve endpoints from: %s. Reason: %s", e, remotesUrl,
                                              e.getMessage() );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }

    private HttpGet newGet( final String url, final ReplicationDTO dto )
    {
        final HttpGet get = new HttpGet( url );
        final int proxyPort = dto.getProxyPort();
        HttpHost proxy;
        if ( proxyPort < 1 )
        {
            proxy = new HttpHost( dto.getProxyHost(), -1, "http" );
        }
        else
        {
            proxy = new HttpHost( dto.getProxyHost(), dto.getProxyPort(), "http" );
        }

        final RequestConfig config = RequestConfig.custom()
                                                  .setProxy( proxy )
                                                  .build();
        get.setConfig( config );

        return get;
    }

    private List<EndpointView> getEndpoints( final ReplicationDTO dto )
        throws IndyWorkflowException
    {
        final String apiUrl = dto.getApiUrl();
        String url = null;
        try
        {
            url = buildUrl( apiUrl, "/stats/all-endpoints" );
        }
        catch ( final MalformedURLException e )
        {
            throw new IndyWorkflowException(
                                              "Failed to construct endpoint-retrieval URL from api-base: {}. Reason: {}",
                                              e, apiUrl, e.getMessage() );
        }

        final HttpGet req = newGet( url, dto );
        CloseableHttpClient client = null;
        try
        {
            String siteId = new URL( url ).getHost();

            client = http.createClient( siteId );

            CloseableHttpResponse response = client.execute( req, http.createContext( siteId ) );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final String json = HttpResources.entityToString( response );
                final EndpointViewListing listing = serializer.readValue( json, EndpointViewListing.class );
                return listing.getItems();
            }

            throw new IndyWorkflowException( status, "Endpoint request failed: {}", statusLine );
        }
        catch ( final IOException | IndyHttpException e )
        {
            throw new IndyWorkflowException( "Failed to retrieve endpoints from: {}. Reason: {}", e, url,
                                              e.getMessage() );
        }
        finally
        {
            IOUtils.closeQuietly( client );
        }
    }

}
