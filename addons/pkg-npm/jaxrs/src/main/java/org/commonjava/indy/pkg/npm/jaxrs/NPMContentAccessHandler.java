/**
 * Copyright (C) 2017 Red Hat, Inc. (yma@commonjava.org)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.pkg.npm.jaxrs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.core.bind.jaxrs.ContentAccessHandler;
import org.commonjava.indy.core.bind.jaxrs.util.TransferStreamingOutput;
import org.commonjava.indy.core.model.StoreHttpExchangeMetadata;
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.npm.inject.NPMContentHandler;
import org.commonjava.indy.util.AcceptInfo;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.TransferManager;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;
import org.commonjava.maven.galley.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.setInfoHeaders;
import static org.commonjava.indy.core.ctl.ContentController.LISTING_HTML_FILE;
import static org.commonjava.indy.pkg.npm.util.GeneratedTransfers.getGeneratedTransfers;
import static org.commonjava.indy.pkg.npm.util.GeneratedTransfers.setGeneratedTransfers;
import static org.commonjava.maven.galley.spi.cache.CacheProvider.STORAGE_PATH;

@ApplicationScoped
@NPMContentHandler
public class NPMContentAccessHandler
        extends ContentAccessHandler
{
    protected final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private TransferManager transfers;

    @Inject
    private ObjectMapper mapper;

    @Override
    public Response doCreate( String packageType, String type, String name, String path, HttpServletRequest request,
                              EventMetadata eventMetadata, Supplier<URI> uriBuilder )
    {
        return doCreate( packageType, type, name, path, request, eventMetadata, uriBuilder, null );
    }

    @Override
    public Response doCreate( String packageType, String type, String name, String path, HttpServletRequest request,
                              EventMetadata eventMetadata, Supplier<URI> uriBuilder,
                              Consumer<Response.ResponseBuilder> builderModifier )
    {
        final StoreType st = StoreType.get( type );
        StoreKey sk = new StoreKey( packageType, st, name );

        eventMetadata = eventMetadata.set( ContentManager.ENTRY_POINT_STORE, sk );

        Response response = null;
        final Transfer transfer;
        try
        {
            InputStream stream = request.getInputStream();
            transfer = contentController.store( sk, path, stream, eventMetadata );

            final StoreKey storageKey = LocationUtils.getKey( transfer );
            logger.info( "Key for storage location: {}", storageKey );

            final URI uri = uriBuilder.get();

            Response.ResponseBuilder builder = Response.created( uri );
            if ( builderModifier != null )
            {
                builderModifier.accept( builder );
            }
            response = builder.build();

            generateNPMContentsFromTransfer( transfer, eventMetadata, response, builderModifier );
            // generating .http-metadata.json for PUT request to resolve some header requirements
            generateHttpMetadataHeaders( transfer, request, response );
        }
        catch ( final IndyWorkflowException | IOException e )
        {
            logger.error( String.format( "Failed to upload: %s to: %s. Reason: %s", path, name, e.getMessage() ), e );

            response = formatResponse( e, builderModifier );
        }

        return response;
    }

    @Override
    public Response doGet( String packageType, String type, String name, String path, String baseUri,
                           HttpServletRequest request, EventMetadata eventMetadata )
    {
        return doGet( packageType, type, name, path, baseUri, request, eventMetadata, null );
    }

    @Override
    public Response doGet( String packageType, String type, String name, String path, String baseUri,
                           HttpServletRequest request, EventMetadata eventMetadata,
                           Consumer<Response.ResponseBuilder> builderModifier )
    {
        if ( !PackageTypes.contains( packageType ) )
        {
            Response.ResponseBuilder builder = Response.status( 400 );
            if ( builderModifier != null )
            {
                builderModifier.accept( builder );
            }
            return builder.build();
        }

         // hide npm sensitive user info for publish
         if ( path.startsWith( "-/user" ) )
         {
             Response.ResponseBuilder builder = Response.status( 404 );
             if ( builderModifier != null )
             {
                builderModifier.accept( builder );
             }
             return builder.build();
         }

        final StoreType st = StoreType.get( type );
        final StoreKey sk = new StoreKey( packageType, st, name );

        eventMetadata = eventMetadata.set( ContentManager.ENTRY_POINT_STORE, sk );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );
        final String standardAccept = ApplicationContent.getStandardAccept( acceptInfo.getBaseAccept() );

        Response response = null;

        logger.info(
                "GET path: '{}' (RAW: '{}')\nIn store: '{}'\nUser addMetadata header is: '{}'\nStandard addMetadata header for that is: '{}'",
                path, request.getPathInfo(), sk, acceptInfo.getRawAccept(), standardAccept );

        if ( path == null || path.equals( "" ) || request.getPathInfo().endsWith( "/" ) || path.endsWith(
                LISTING_HTML_FILE ) )
        {
            try
            {
                logger.info( "Getting listing at: {}", path );
                final String content =
                        contentController.renderListing( standardAccept, sk, path, baseUri, uriFormatter, eventMetadata );

                response = formatOkResponseWithEntity( content, acceptInfo.getRawAccept(), builderModifier );
            }
            catch ( final IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to render content listing: %s from: %s. Reason: %s", path, name,
                                             e.getMessage() ), e );
                response = formatResponse( e, builderModifier );
            }
        }
        else
        {
            try
            {
                if ( eventMetadata.get( STORAGE_PATH ) != null && StoreType.remote != st )
                {
                    // make sure the right mapping path for hosted and group when retrieve content
                    path = PathUtils.storagePath( path, eventMetadata );
                }
                logger.info( "START: retrieval of content: {}:{}", sk, path );
                final Transfer item = contentController.get( sk, path, eventMetadata );

                logger.info( "HANDLE: retrieval of content: {}:{}", sk, path );
                if ( item == null )
                {
                    return handleMissingContentQuery( sk, path, builderModifier );
                }

                boolean handleLocking = false;
                if ( !item.isWriteLocked() )
                {
                    item.lockWrite();
                    handleLocking = true;
                }

                try
                {
                    if ( !item.exists() )
                    {
                        return handleMissingContentQuery( sk, path, builderModifier );
                    }
                    else if ( item.isDirectory() )
                    {
                        try
                        {
                            logger.info( "Getting listing at: {}", path + "/" );
                            final String content =
                                    contentController.renderListing( standardAccept, sk, path + "/", baseUri,
                                                                     uriFormatter, eventMetadata );

                            response =
                                    formatOkResponseWithEntity( content, acceptInfo.getRawAccept(), builderModifier );
                        }
                        catch ( final IndyWorkflowException e )
                        {
                            logger.error(
                                    String.format( "Failed to render content listing: %s from: %s. Reason: %s", path,
                                                   name, e.getMessage() ), e );
                            response = formatResponse( e, builderModifier );
                        }
                    }
                    else
                    {
                        logger.info( "RETURNING: retrieval of content: {}:{}", sk, path );
                        // open the stream here to prevent deletion while waiting for the transfer back to the user to start...
                        InputStream in = item.openInputStream( true, eventMetadata );
                        final Response.ResponseBuilder builder = Response.ok( new TransferStreamingOutput( in ) );
                        setInfoHeaders( builder, item, sk, path, true, contentController.getContentType( path ),
                                        contentController.getHttpMetadata( item ) );
                        if ( builderModifier != null )
                        {
                            builderModifier.accept( builder );
                        }
                        response = builder.build();
                        // generating .http-metadata.json for npm group retrieve to resolve header requirements
                        if ( eventMetadata.get( STORAGE_PATH ) != null && StoreType.group == st )
                        {
                            generateHttpMetadataHeaders( item, request, response );
                        }
                    }
                }
                finally
                {
                    if ( handleLocking )
                    {
                        item.unlock();
                    }
                }
            }
            catch ( final IOException | IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to download artifact: %s from: %s. Reason: %s", path, name,
                                             e.getMessage() ), e );
                response = formatResponse( e, builderModifier );
            }
        }

        logger.info( "RETURNING RESULT: {}:{}", sk, path );
        return response;
    }

    private void generateNPMContentsFromTransfer( final Transfer transfer, final EventMetadata eventMetadata,
                                                  Response response,
                                                  final Consumer<Response.ResponseBuilder> builderModifier )
    {
        if ( transfer == null )
        {
            return;
        }

        OutputStream out1 = null;
        OutputStream out2 = null;

        Transfer target1;
        Transfer target2;
        String content1 = "";
        String content2 = "";

        ConcreteResource resource = transfer.getResource();
        try
        {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree( transfer.openInputStream( true ) );

            String path1 = null;
            String path2 = null;
            JsonNode vnode = root.path( "versions" );
            JsonNode anode = root.path( "_attachments" );
            JsonNode idnode = root.path( "_id" );

            if ( vnode.fields().hasNext() )
            {
                String version = vnode.fields().next().getKey();
                if ( version == null )
                {
                    return;
                }
                path1 = Paths.get( idnode.asText(), version ).toString();
                content1 = vnode.findValue( version ).toString();
            }

            if ( anode.fields().hasNext() )
            {
                String tarball = anode.fields().next().getKey();
                if ( tarball == null )
                {
                    return;
                }
                path2 = Paths.get( idnode.asText(), "-", tarball ).toString();
                content2 = anode.findPath( "data" ).asText();
            }

            if ( path1 == null || path2 == null )
            {
                return;
            }
            target1 = transfers.getCacheReference( new ConcreteResource( resource.getLocation(), path1 ) );
            logger.info( "STORE {}", target1.getResource() );

            target2 = transfers.getCacheReference( new ConcreteResource( resource.getLocation(), path2 ) );
            logger.info( "STORE {}", target2.getResource() );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "[NPM] Json node parse failed for resource: %s. Reason: %s", resource,
                                         e.getMessage() ), e );
            response = formatResponse( e, builderModifier );

            return;
        }
        try
        {
            if ( target1 == null || target2 == null )
            {
                return;
            }
            out1 = target1.openOutputStream( TransferOperation.UPLOAD, true, eventMetadata );
            out1.write( content1.getBytes() );

            out2 = target2.openOutputStream( TransferOperation.UPLOAD, true, eventMetadata );
            out2.write( Base64.decodeBase64( content2 ) );

            setGeneratedTransfers( transfer, target1, target2 );
        }
        catch ( final IOException e )
        {
            logger.error( String.format( "[NPM] Failed to store the generated targets: s% and s%. Reason: s%",
                                         target1.getResource(), target2.getResource(), e.getMessage() ), e );
            response = formatResponse( e, builderModifier );
        }
        finally
        {
            closeQuietly( out1 );
            closeQuietly( out2 );
        }
    }

    public void generateHttpMetadataHeaders( final Transfer transfer, final HttpServletRequest request,
                                             final Response response )
    {
        if ( transfer == null || request == null || response == null )
        {
            return;
        }

        Response responseWithLastModified =
                response.fromResponse( response ).lastModified( new Date( transfer.lastModified() ) ).build();

        Transfer metaTxfr = transfer.getSiblingMeta( HttpExchangeMetadata.FILE_EXTENSION );
        if ( metaTxfr == null )
        {
            if ( transfer.isDirectory() )
            {
                metaTxfr = transfer.getChild( HttpExchangeMetadata.FILE_EXTENSION );
            }
            else
            {
                return;
            }
        }

        final HttpExchangeMetadata metadata = new StoreHttpExchangeMetadata( request, responseWithLastModified );
        OutputStream out = null;
        try
        {
            out = metaTxfr.openOutputStream( TransferOperation.GENERATE, false );
            out.write( mapper.writeValueAsBytes( metadata ) );
        }
        catch ( final IOException e )
        {
            logger.error( "Failed to write metadata for HTTP exchange to: {}. Reason: {}", metaTxfr, e );
        }
        finally
        {
            IOUtils.closeQuietly( out );
        }

        // npm will generate .tgz and version json metadata files from the package json file target,
        // which will also need the HttpExchangeMetadata for npm header check.

        List<Transfer> generated = getGeneratedTransfers( transfer );
        if ( generated == null )
        {
            return;
        }
        for ( Transfer t : generated )
        {
            generateHttpMetadataHeaders( t, request, response );
        }
    }
}
