/**
 * Copyright (C) 2011 Red Hat, Inc. (yma@commonjava.org)
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

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.core.bind.jaxrs.ContentAccessHandler;
import org.commonjava.indy.core.bind.jaxrs.util.TransferStreamingOutput;
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.model.util.HttpUtils;
import org.commonjava.indy.pkg.npm.content.group.PackageMetadataMerger;
import org.commonjava.indy.util.AcceptInfo;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.ApplicationHeader;
import org.commonjava.maven.atlas.ident.util.VersionUtils;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.commonjava.maven.galley.transport.htcli.model.HttpExchangeMetadata;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.function.Consumer;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponseFromMetadata;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.setInfoHeaders;

public class PackageContentAccessHandler
                extends ContentAccessHandler
{
    public static final String CROSS = "-";

    public static final String PATH_SPLITER = "/";

    public static final String TGZ = ".tgz";

    public static final String PACAGE_TGZ = "package" + TGZ;

    @Override
    public Response doHead( final String packageType, final String type, final String name, final String packageName,
                            final Boolean cacheOnly, final String baseUri, final HttpServletRequest request,
                            EventMetadata eventMetadata, final Consumer<Response.ResponseBuilder> builderModifier )
    {
        return doHead( packageType, type, name, packageName, null, null, cacheOnly, baseUri, request, eventMetadata,
                       builderModifier );
    }

    @Override
    public Response doHead( final String packageType, final String type, final String name, final String packageName,
                            final Boolean cacheOnly, final String baseUri, final HttpServletRequest request,
                            EventMetadata eventMetadata )
    {
        return doHead( packageType, type, name, packageName, null, null, cacheOnly, baseUri, request, eventMetadata,
                       null );
    }

    public Response doHead( final String packageType, final String type, final String name, final String packageName,
                            final String version, final String tarball, final Boolean cacheOnly, final String baseUri,
                            final HttpServletRequest request, EventMetadata eventMetadata )
    {
        return doHead( packageType, type, name, packageName, version, tarball, cacheOnly, baseUri, request,
                       eventMetadata, null );
    }

    public Response doHead( final String packageType, final String type, final String name, final String packageName,
                            final String version, final String tarball, final Boolean cacheOnly, final String baseUri,
                            final HttpServletRequest request, EventMetadata eventMetadata,
                            final Consumer<Response.ResponseBuilder> builderModifier )
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

        final StoreType st = StoreType.get( type );
        final StoreKey sk = new StoreKey( packageType, st, name );

        eventMetadata = eventMetadata.set( ContentManager.ENTRY_POINT_STORE, sk );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );

        Response response = null;

        if ( packageName == null || packageName.trim().length() < 1 )
        {
            try
            {
                logger.info( "Getting root listing" );
                final String content =
                                contentController.renderListing( acceptInfo.getBaseAccept(), sk, packageName, baseUri,
                                                                 uriFormatter );

                Response.ResponseBuilder builder = Response.ok()
                                                           .header( ApplicationHeader.content_type.key(),
                                                                    acceptInfo.getRawAccept() )
                                                           .header( ApplicationHeader.content_length.key(),
                                                                    Long.toString( content.length() ) )
                                                           .header( ApplicationHeader.last_modified.key(),
                                                                    HttpUtils.formatDateHeader( new Date() ) );
                if ( builderModifier != null )
                {
                    builderModifier.accept( builder );
                }
                return builder.build();
            }
            catch ( final IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to render content root listing: from: %s. Reason: %s", name,
                                             e.getMessage() ), e );
                return formatResponse( e, builderModifier );
            }
        }

        String path = null;

        // map /{package} to /{package}/package.json
        if ( version == null || version.trim().length() < 1 )
        {
            path = packageName + PATH_SPLITER + PackageMetadataMerger.METADATA_NAME;
        }
        // map /{package}/{version} to /{package}/{version}/package.json
        if ( VersionUtils.isValidSingleVersion( version ) )
        {
            path = packageName + PATH_SPLITER + version + PATH_SPLITER + PackageMetadataMerger.METADATA_NAME;
        }
        // map /{package}/-/{package}-{version}.tgz to /{package}/{version}/package.tgz
        if ( version.equals( CROSS ) && tarball != null && tarball.trim().length() >= 1 )
        {
            String versionSub;
            try
            {
                versionSub = tarball.substring( packageName.length() + CROSS.length(), tarball.length() - 4 );
                if ( versionSub.isEmpty() )
                {
                    return response;
                }
            }
            catch ( Exception e )
            {
                return response;
            }
            path = packageName + PATH_SPLITER + versionSub + PATH_SPLITER + PACAGE_TGZ;
        }

        try
        {
            if ( path == null )
            {
                return response;
            }
            Transfer item = null;
            logger.info( "Checking existence of: {}:{} (cache only? {})", sk, path, cacheOnly );

            boolean exists = false;
            if ( Boolean.TRUE.equals( cacheOnly ) )
            {
                logger.debug( "Calling getTransfer()" );
                item = contentController.getTransfer( sk, path, TransferOperation.DOWNLOAD );
                exists = item != null && item.exists();
                logger.debug( "Got transfer reference: {}", item );
            }
            else
            {
                // Use exists for remote repo to avoid downloading file. Use getTransfer for everything else (hosted, cache-only).
                // Response will be composed of metadata by getHttpMetadata which get metadata from .http-metadata.json (because HTTP transport always writes a .http-metadata.json
                // file when it makes a request). This file stores the HTTP response status code and headers regardless exist returning true or false.
                logger.debug( "Calling exists()" );
                exists = contentController.exists( sk, path );
                logger.debug( "Got exists: {}", exists );
            }

            if ( exists )
            {
                HttpExchangeMetadata httpMetadata = item != null ?
                                contentController.getHttpMetadata( item ) :
                                contentController.getHttpMetadata( sk, path );

                if ( httpMetadata == null )
                {
                    logger.info( "Retrieving: {}:{} for existence test", sk, path );
                    item = contentController.get( sk, path, eventMetadata );
                    logger.debug( "Got retrieved transfer reference: {}", item );
                }

                logger.debug( "Building 200 response. Using HTTP metadata: {}", httpMetadata );

                final Response.ResponseBuilder builder = Response.ok();
                setInfoHeaders( builder, item, sk, path, true, contentController.getContentType( path ), httpMetadata );
                if ( builderModifier != null )
                {
                    builderModifier.accept( builder );
                }
                response = builder.build();
            }
            else
            {
                logger.debug( "Building 404 (or error) response..." );
                if ( StoreType.remote == st )
                {
                    final HttpExchangeMetadata metadata = contentController.getHttpMetadata( sk, path );
                    if ( metadata != null )
                    {
                        logger.debug( "Using HTTP metadata to build negative response." );
                        response = formatResponseFromMetadata( metadata );
                    }
                }

                if ( response == null )
                {
                    logger.debug( "No HTTP metadata; building generic 404 response." );
                    Response.ResponseBuilder builder = Response.status( Response.Status.NOT_FOUND );
                    if ( builderModifier != null )
                    {
                        builderModifier.accept( builder );
                    }
                    response = builder.build();
                }
            }
        }
        catch ( final IndyWorkflowException e )
        {
            logger.error( String.format( "Failed to download package / metadata content file: %s from: %s. Reason: %s",
                                         path, name, e.getMessage() ), e );
            response = formatResponse( e, builderModifier );
        }
        return response;
    }

    @Override
    public Response doGet( final String packageType, final String type, final String name, final String packageName,
                           final String baseUri, final HttpServletRequest request, EventMetadata eventMetadata,
                           final Consumer<Response.ResponseBuilder> builderModifier )
    {
        return doGet( packageType, type, name, packageName, null, null, baseUri, request, eventMetadata,
                      builderModifier );
    }

    @Override
    public Response doGet( final String packageType, final String type, final String name, final String packageName,
                           final String baseUri, final HttpServletRequest request, EventMetadata eventMetadata )
    {
        return doGet( packageType, type, name, packageName, null, null, baseUri, request, eventMetadata, null );
    }

    public Response doGet( final String packageType, final String type, final String name, final String packageName,
                           final String version, final String tarball, final String baseUri,
                           final HttpServletRequest request, EventMetadata eventMetadata )
    {
        return doGet( packageType, type, name, packageName, version, tarball, baseUri, request, eventMetadata, null );
    }

    public Response doGet( final String packageType, final String type, final String name, final String packageName,
                           final String version, final String tarball, final String baseUri,
                           final HttpServletRequest request, EventMetadata eventMetadata,
                           final Consumer<Response.ResponseBuilder> builderModifier )
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

        final StoreType st = StoreType.get( type );
        final StoreKey sk = new StoreKey( packageType, st, name );

        eventMetadata = eventMetadata.set( ContentManager.ENTRY_POINT_STORE, sk );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );
        final String standardAccept = ApplicationContent.getStandardAccept( acceptInfo.getBaseAccept() );

        Response response = null;

        logger.info( "GET package: '{}' (RAW: '{}')\nIn store: '{}'\nUser addMetadata header is: '{}'\nStandard addMetadata header for that is: '{}'",
                     packageName, request.getPathInfo(), sk, acceptInfo.getRawAccept(), standardAccept );

        if ( packageName == null || packageName.trim().length() < 1 )
        {
            try
            {
                logger.info( "Getting root listing" );
                final String content = contentController.renderListing( standardAccept, st, name, packageName, baseUri,
                                                                        uriFormatter );

                return formatOkResponseWithEntity( content, acceptInfo.getRawAccept(), builderModifier );
            }
            catch ( final IndyWorkflowException e )
            {
                logger.error( String.format( "Failed to render content root listing: from: %s. Reason: %s", name,
                                             e.getMessage() ), e );
                return formatResponse( e, builderModifier );
            }
        }

        String path = null;
        // map /{package} to /{package}/package.json
        if ( version == null || version.trim().length() < 1 )
        {
            path = packageName + PATH_SPLITER + PackageMetadataMerger.METADATA_NAME;
        }
        // map /{package}/{version} to /{package}/{version}/package.json
        if ( VersionUtils.isValidSingleVersion( version ) )
        {
            path = packageName + PATH_SPLITER + version + PATH_SPLITER + PackageMetadataMerger.METADATA_NAME;
        }
        // map /{package}/-/{package}-{version}.tgz to /{package}/{version}/package.tgz
        if ( version.equals( CROSS ) && tarball != null && tarball.trim().length() >= 1 )
        {
            String versionSub;
            try
            {
                versionSub = tarball.substring( packageName.length() + CROSS.length(), tarball.length() - 4 );
                if ( versionSub.isEmpty() )
                {
                    return response;
                }
            }
            catch ( Exception e )
            {
                return response;
            }
            path = packageName + PATH_SPLITER + versionSub + PATH_SPLITER + PACAGE_TGZ;
        }

        try
        {
            if ( path == null )
            {
                return response;
            }

            logger.info( "START: retrieval of content: {}:{}", sk, path );
            Transfer item = contentController.get( sk, path, eventMetadata );

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

                logger.info( "RETURNING: retrieval of content: {}:{}", sk, path );
                Object entity = null;
                // open the stream here to prevent deletion while waiting for the transfer back to the user to start...
                InputStream in = item.openInputStream( true, eventMetadata );

                if ( path.endsWith( PACAGE_TGZ ) )
                {
                    entity = new TransferStreamingOutput( in );
                }
                else
                {
                    entity = IOUtils.toString( in );
                }

                final Response.ResponseBuilder builder = Response.ok( entity );
                setInfoHeaders( builder, item, sk, path, true, contentController.getContentType( path ),
                                contentController.getHttpMetadata( item ) );

                if ( builderModifier != null )
                {
                    builderModifier.accept( builder );
                }
                response = builder.build();
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
            logger.error( String.format( "Failed to download package metadata: %s from: %s. Reason: %s", path, name,
                                         e.getMessage() ), e );
            response = formatResponse( e, builderModifier );
        }

        return response;
    }
}
