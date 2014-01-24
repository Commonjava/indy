/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.rest;

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
import org.commonjava.aprox.core.dto.repl.ReplicationAction;
import org.commonjava.aprox.core.dto.repl.ReplicationAction.ActionType;
import org.commonjava.aprox.core.dto.repl.ReplicationDTO;
import org.commonjava.aprox.core.model.EndpointView;
import org.commonjava.aprox.core.model.EndpointViewListing;
import org.commonjava.aprox.data.ProxyDataException;
import org.commonjava.aprox.data.StoreDataManager;
import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.DeployPoint;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.Repository;
import org.commonjava.aprox.model.StoreKey;
import org.commonjava.aprox.model.StoreType;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.aprox.subsys.http.AproxHttpProvider;
import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.model.Listing;
import org.commonjava.web.json.ser.JsonSerializer;

import com.google.gson.reflect.TypeToken;

@ApplicationScoped
public class ReplicationController
{
    private final Logger logger = new Logger( getClass() );

    @Inject
    private StoreDataManager data;

    @Inject
    @AproxData
    private JsonSerializer serializer;

    @Inject
    private AproxHttpProvider http;

    protected ReplicationController()
    {
    }

    public ReplicationController( final StoreDataManager data, final AproxHttpProvider http, final JsonSerializer serializer )
    {
        this.data = data;
        this.http = http;
        this.serializer = serializer;
    }

    public Set<StoreKey> replicate( final ReplicationDTO dto )
        throws AproxWorkflowException
    {
        dto.validate();

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

            logger.info( "Processing replication action:\n\n  %s\n\nin DTO: %s\n\n", action, dto );
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
                            final StoreKey sk = new StoreKey( StoreType.repository, key );
                            if ( overwrite || !data.hasArtifactStore( sk ) )
                            {
                                final Repository repo = new Repository( key, view.getResourceURI() );
                                setProxyAttributes( repo, action );

                                data.storeRepository( repo );
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
                                if ( store instanceof Repository )
                                {
                                    setProxyAttributes( ( (Repository) store ), action );
                                }

                                data.storeArtifactStore( store );
                                replicated.add( store.getKey() );
                            }
                        }
                    }
                }
            }
            catch ( final ProxyDataException e )
            {
                logger.error( e.getMessage(), e );
                throw new AproxWorkflowException( e.getMessage(), e );
            }
        }

        return replicated;
    }

    private void setProxyAttributes( final Repository repo, final ReplicationAction action )
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

        String reposUrl = null;
        String groupsUrl = null;
        String deploysUrl = null;
        try
        {
            reposUrl = buildUrl( apiUrl, "/admin/repositories" );
            groupsUrl = buildUrl( apiUrl, "/admin/groups" );
            deploysUrl = buildUrl( apiUrl, "/admin/deploys" );
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException( "Failed to construct store definition-retrieval URL from api-base: %s. Reason: %s", e, apiUrl,
                                              e.getMessage() );
        }

        //        logger.info( "\n\n\n\n\n[AutoProx] Checking URL: %s from:", new Throwable(), url );
        final List<ArtifactStore> result = new ArrayList<ArtifactStore>();

        HttpGet req = newGet( reposUrl, dto );

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

                final Listing<Repository> listing = serializer.fromString( json, new TypeToken<Listing<Repository>>()
                {
                }.getType() );

                if ( listing != null )
                {
                    for ( final Repository store : listing.getItems() )
                    {
                        result.add( store );
                    }
                }
            }
            else
            {
                throw new AproxWorkflowException( status, "Request: %s failed: %s", reposUrl, statusLine );
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: %s. Reason: %s", e, reposUrl, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: %s. Reason: %s", e, reposUrl, e.getMessage() );
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
                throw new AproxWorkflowException( status, "Request: %s failed: %s", groupsUrl, statusLine );
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: %s. Reason: %s", e, groupsUrl, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: %s. Reason: %s", e, groupsUrl, e.getMessage() );
        }
        finally
        {
            http.closeConnection();
        }

        req = newGet( deploysUrl, dto );

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

                final Listing<DeployPoint> listing = serializer.fromString( json, new TypeToken<Listing<DeployPoint>>()
                {
                }.getType() );

                for ( final DeployPoint store : listing.getItems() )
                {
                    result.add( store );
                }
            }
            else
            {
                throw new AproxWorkflowException( status, "Request: %s failed: %s", deploysUrl, statusLine );
            }
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: %s. Reason: %s", e, deploysUrl, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: %s. Reason: %s", e, deploysUrl, e.getMessage() );
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
            throw new AproxWorkflowException( "Failed to construct endpoint-retrieval URL from api-base: %s. Reason: %s", e, apiUrl, e.getMessage() );
        }

        //        logger.info( "\n\n\n\n\n[AutoProx] Checking URL: %s from:", new Throwable(), url );
        final HttpGet req = newGet( url, dto );

        try
        {
            final HttpResponse response = http.getClient()
                                              .execute( req );

            final StatusLine statusLine = response.getStatusLine();
            final int status = statusLine.getStatusCode();
            if ( status == HttpStatus.SC_OK )
            {
                final EndpointViewListing listing = serializer.fromStream( response.getEntity()
                                                                                   .getContent(), "UTF-8", EndpointViewListing.class );

                return listing.getItems();
            }

            throw new AproxWorkflowException( status, "Endpoint request failed: %s", statusLine );
        }
        catch ( final ClientProtocolException e )
        {
            throw new AproxWorkflowException( "Failed to retrieve endpoints from: %s. Reason: %s", e, url, e.getMessage() );
        }
        catch ( final IOException e )
        {
            throw new AproxWorkflowException( "Failed to read endpoints from: %s. Reason: %s", e, url, e.getMessage() );
        }
        finally
        {
            http.closeConnection();
        }
    }

}
