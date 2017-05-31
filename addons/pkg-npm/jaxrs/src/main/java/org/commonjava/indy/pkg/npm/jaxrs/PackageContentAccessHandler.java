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

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.content.ContentManager;
import org.commonjava.indy.core.bind.jaxrs.ContentAccessHandler;
import org.commonjava.indy.core.bind.jaxrs.util.TransferStreamingOutput;
import org.commonjava.indy.model.core.PackageTypes;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.model.core.StoreType;
import org.commonjava.indy.pkg.npm.content.group.PackageMetadataMerger;
import org.commonjava.indy.util.AcceptInfo;
import org.commonjava.indy.util.ApplicationContent;
import org.commonjava.indy.util.ApplicationStatus;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.model.Transfer;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;

import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatOkResponseWithEntity;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.formatResponse;
import static org.commonjava.indy.bind.jaxrs.util.ResponseUtils.setInfoHeaders;

public class PackageContentAccessHandler
                extends ContentAccessHandler
{

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

        final StoreType st = StoreType.get( type );
        final StoreKey sk = new StoreKey( packageType, st, name );

        eventMetadata = eventMetadata.set( ContentManager.ENTRY_POINT_STORE, sk );

        final AcceptInfo acceptInfo = jaxRsRequestHelper.findAccept( request, ApplicationContent.text_html );
        final String standardAccept = ApplicationContent.getStandardAccept( acceptInfo.getBaseAccept() );

        Response response = null;

        logger.info( "GET path: '{}' (RAW: '{}')\nIn store: '{}'\nUser addMetadata header is: '{}'\nStandard addMetadata header for that is: '{}'",
                     path, request.getPathInfo(), sk, acceptInfo.getRawAccept(), standardAccept );

        if ( path == null || path.equals( "" ) )
        {
            try
            {
                logger.info( "Getting listing at: {}", path );
                final String content = contentController.renderListing( standardAccept, st, name, path, baseUri,
                                                                        uriFormatter );

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
                /**
                 * make path: package/ rendered to path: package/package.json
                 * make path: package/version rendered to path: package/version/package.json
                 * make path: package/-/package**.tgz, path: package/(version/)package.json not rendered
                 */
                if ( request.getPathInfo().endsWith( "/" ) )
                {
                    path = path + PackageMetadataMerger.METADATA_NAME;
                }
                else if ( !path.endsWith( PackageMetadataMerger.METADATA_NAME ) && !path.contains( "/-/" ) )
                {
                    path = path + "/" + PackageMetadataMerger.METADATA_NAME;
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
                    else if ( item.isDirectory() )
                    {
                        response = formatResponse( ApplicationStatus.METHOD_NOT_ALLOWED, null,
                                                   "Rendered content listing is not allowed.", builderModifier );
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
}
