/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.core.ctl;

import static org.commonjava.aprox.util.UrlUtils.buildUrl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.audit.SecurityAction;
import org.commonjava.aprox.audit.SecuritySystem;
import org.commonjava.aprox.audit.SecuritySystemException;
import org.commonjava.aprox.audit.SimpleSecurityAction;
import org.commonjava.aprox.core.dto.ReplicationAction;
import org.commonjava.aprox.core.dto.ReplicationAction.ActionType;
import org.commonjava.aprox.core.dto.ReplicationDTO;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.dto.EndpointView;
import org.commonjava.aprox.dto.EndpointViewListing;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.reflect.TypeToken;

@ApplicationScoped
public class ReplicationController
{
    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager data;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private AproxHttpProvider http;

    @Inject
    private SecuritySystem securitySystem;

    protected ReplicationController()
    {
    }

    public ReplicationController( final StoreDataManager data, final AproxHttpProvider http,
                                  final JsonSerializer serializer, final SecuritySystem securitySystem )
    {
        this.data = data;
        this.http = http;
        this.serializer = serializer;
        this.securitySystem = securitySystem;
    }

    public Set<StoreKey> replicate( final ReplicationDTO dto )
        throws AproxWorkflowException
    {
        final SecurityAction<Set<StoreKey>, AproxWorkflowException> action =
            new SimpleSecurityAction<Set<StoreKey>, AproxWorkflowException>()
            {
                @Override
                public Set<StoreKey> run()
                {
                    final Set<StoreKey> replicated = new HashSet<StoreKey>();
                    try
                    {
                        dto.validate();
                    }
                    catch ( final AproxWorkflowException e )
                    {
                        setError( e );
                        return replicated;
                    }

                    List<? extends ArtifactStore> remoteStores = null;
                    List<EndpointView> remoteEndpoints = null;

                    final boolean overwrite = dto.isOverwrite();
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
                                    if ( ( include == null || key.matches( include ) ) && ( exclude == null || !key.matches( exclude ) ) )
                                    {
                                        final StoreKey sk = new StoreKey( StoreType.remote, key );
                                        if ( overwrite || !data.hasArtifactStore( sk ) )
                                        {
                                            final RemoteRepository repo = new RemoteRepository( key, view.getResourceURI() );
                                            setProxyAttributes( repo, action );

                                            data.storeRemoteRepository( repo,
                                                                        "REPLICATION: Proxying remote aprox repository: "
                                                                            + view.getResourceURI() );
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
                                    if ( ( include == null || key.matches( include ) ) && ( exclude == null || !key.matches( exclude ) ) )
                                    {
                                        if ( overwrite || !data.hasArtifactStore( store.getKey() ) )
                                        {
                                            if ( store instanceof RemoteRepository )
                                            {
                                                setProxyAttributes( ( (RemoteRepository) store ), action );
                                            }

                                            data.storeArtifactStore( store,
                                                                     "REPLICATION: Mirroring remote aprox store: "
                                                                         + store.getKey() );
                                            replicated.add( store.getKey() );
                                        }
                                    }
                                }
                            }
                        }
                        catch ( final ProxyDataException e )
                        {
                            logger.error( e.getMessage(), e );
                            setError( new AproxWorkflowException( e.getMessage(), e ) );
                        }
                        catch ( final AproxWorkflowException e )
                        {
                            setError( e );
                        }
                    }
                    
                    return replicated;
                }
            };

        Set<StoreKey> replicated;
        try
        {
            replicated = securitySystem.runAsCurrentPrincipal( action );
        }
        catch ( final SecuritySystemException e )
        {
            throw new AproxWorkflowException( "Replication failed.", e.getSecurityException() );
        }

        final AproxWorkflowException e = action.getError();
        if ( e != null )
        {
            throw e;
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
        throws AproxWorkflowException
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
            throw new AproxWorkflowException(
                                              "Failed to construct store definition-retrieval URL from api-base: {}. Reason: {}",
                                              e, apiUrl, e.getMessage() );
        }

        //        logger.info( "\n\n\n\n\n[AutoProx] Checking URL: {} from:", new Throwable(), url );
        final List<ArtifactStore> result = new ArrayList<ArtifactStore>();

        HttpGet req = newGet( remotesUrl, dto );

        try
        {
            final HttpResponse response = http.getClient()
                                              .execute( req );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final String json = IOUtils.toString( response.getEntity()
                                                              .getContent() );

                final Listing<RemoteRepository> listing =
                    serializer.fromString( json, new TypeToken<Listing<RemoteRepository>>()
                    {
                    }.getType() );

                if ( listing != null )
                {
                    for ( final RemoteRepository store : listing.getItems() )
                    {
                        result.add( store );
                    }
                }
            }
            else
            {
                throw new AproxWorkflowException( status, "Request: %s failed: %s", remotesUrl, statusLine );
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: %s. Reason: %s", e, remotesUrl,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: %s. Reason: %s", e, remotesUrl,
                                              e.getMessage() );
        }
        finally
        {
            http.closeConnection();
        }

        req = newGet( groupsUrl, dto );

        try
        {
            final HttpResponse response = http.getClient()
                                              .execute( req );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final String json = IOUtils.toString( response.getEntity()
                                                              .getContent() );

                final Listing<Group> listing = serializer.fromString( json, new TypeToken<Listing<Group>>()
                {
                }.getType() );

                for ( final Group store : listing.getItems() )
                {
                    result.add( store );
                }
            }
            else
            {
                throw new AproxWorkflowException( status, "Request: {} failed: {}", groupsUrl, statusLine );
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: {}. Reason: {}", e, groupsUrl,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: {}. Reason: {}", e, groupsUrl,
                                              e.getMessage() );
        }
        finally
        {
            http.closeConnection();
        }

        req = newGet( hostedUrl, dto );

        try
        {
            final HttpResponse response = http.getClient()
                                              .execute( req );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final String json = IOUtils.toString( response.getEntity()
                                                              .getContent() );

                final Listing<HostedRepository> listing =
                    serializer.fromString( json, new TypeToken<Listing<HostedRepository>>()
                    {
                    }.getType() );

                for ( final HostedRepository store : listing.getItems() )
                {
                    result.add( store );
                }
            }
            else
            {
                throw new AproxWorkflowException( status, "Request: %s failed: %s", hostedUrl, statusLine );
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: %s. Reason: %s", e, hostedUrl,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: %s. Reason: %s", e, hostedUrl,
                                              e.getMessage() );
        }
        finally
        {
            http.closeConnection();
        }

        return result;
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

        get.getParams()
           .setParameter( ConnRoutePNames.DEFAULT_PROXY, proxy );

        return get;
    }

    private List<EndpointView> getEndpoints( final ReplicationDTO dto )
        throws AproxWorkflowException
    {
        final String apiUrl = dto.getApiUrl();
        String url = null;
        try
        {
            url = buildUrl( apiUrl, "/stats/all-endpoints" );
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException(
                                              "Failed to construct endpoint-retrieval URL from api-base: {}. Reason: {}",
                                              e, apiUrl, e.getMessage() );
        }

        //        logger.info( "\n\n\n\n\n[AutoProx] Checking URL: {} from:", new Throwable(), url );
        final HttpGet req = newGet( url, dto );

        try
        {
            final HttpResponse response = http.getClient()
                                              .execute( req );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final EndpointViewListing listing =
                    serializer.fromStream( response.getEntity()
                                                   .getContent(), "UTF-8", EndpointViewListing.class );

                return listing.getItems();
            }

            throw new AproxWorkflowException( status, "Endpoint request failed: {}", statusLine );
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: {}. Reason: {}", e, url,
                                              e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: {}. Reason: {}", e, url, e.getMessage() );
        }
        finally
        {
            http.closeConnection();
        }
    }

}
