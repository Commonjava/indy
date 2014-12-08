package org.commonjava.aprox.setback.vertx;

import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.bind.vertx.util.PathParam;
import org.commonjava.aprox.bind.vertx.util.ResponseUtils;
import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.commonjava.aprox.setback.rest.SetBackController;
import org.commonjava.aprox.subsys.datafile.DataFile;
import org.commonjava.aprox.util.ApplicationContent;
import org.commonjava.aprox.util.ApplicationHeader;
import org.commonjava.aprox.util.ApplicationStatus;
import org.commonjava.vertx.vabr.anno.Handles;
import org.commonjava.vertx.vabr.anno.Route;
import org.commonjava.vertx.vabr.helper.RequestHandler;
import org.commonjava.vertx.vabr.types.Method;
import org.vertx.java.core.http.HttpServerRequest;

@Handles( "/setback" )
public class SetBackSettingsResource
    implements RequestHandler
{

    @Inject
    private SetBackController controller;

    @Route( path = "/:type=(remote|group)/:name", method = Method.GET, contentType = ApplicationContent.application_xml )
    public void get( final HttpServerRequest request )
    {
        final String t = request.params()
                                .get( PathParam.type.key() );
        final String n = request.params()
                                .get( PathParam.name.key() );

        final StoreType type = StoreType.get( t );

        if ( StoreType.hosted == type )
        {
            ResponseUtils.setStatus( ApplicationStatus.BAD_REQUEST, request );
            request.response()
                   .end();
            return;
        }

        final StoreKey key = new StoreKey( type, n );
        DataFile settingsXml;
        try
        {
            settingsXml = controller.getSetBackSettings( key );
        }
        catch ( final AproxWorkflowException e )
        {
            ResponseUtils.formatResponse( e, request );
            return;
        }

        if ( settingsXml != null && settingsXml.exists() )
        {
            ResponseUtils.setStatus( ApplicationStatus.OK, request );
            request.response()
                   .putHeader( ApplicationHeader.content_type.key(), ApplicationContent.application_xml );

            request.response()
                   .sendFile( settingsXml.getPath() )
                   .close();
        }
        else
        {
            ResponseUtils.setStatus( ApplicationStatus.NOT_FOUND, request );
            request.response()
                   .end();
        }
    }

    @Route( path = "/:type=(remote|group)/:name", method = Method.DELETE, contentType = ApplicationContent.application_xml )
    public void delete( final HttpServerRequest request )
    {
        final String t = request.params()
                                .get( PathParam.type.key() );
        final String n = request.params()
                                .get( PathParam.name.key() );

        final StoreType type = StoreType.get( t );

        if ( StoreType.hosted == type )
        {
            ResponseUtils.setStatus( ApplicationStatus.BAD_REQUEST, request );
            request.response()
                   .end();
            return;
        }

        final StoreKey key = new StoreKey( type, n );
        boolean found;
        try
        {
            found = controller.deleteSetBackSettings( key );
        }
        catch ( final AproxWorkflowException e )
        {
            ResponseUtils.formatResponse( e, request );
            return;
        }

        if ( found )
        {
            ResponseUtils.setStatus( ApplicationStatus.OK, request );
            request.response()
                   .end();
        }
        else
        {
            ResponseUtils.setStatus( ApplicationStatus.NOT_FOUND, request );
            request.response()
                   .end();
        }
    }

}
