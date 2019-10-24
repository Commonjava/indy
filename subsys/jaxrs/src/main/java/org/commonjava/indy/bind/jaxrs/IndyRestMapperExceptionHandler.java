package org.commonjava.indy.bind.jaxrs;


import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonObjectFormatVisitor;
import org.commonjava.indy.IndyException;
import org.commonjava.indy.model.rest.IndyRestMapperResponse;
import org.jboss.resteasy.client.exception.ResteasyHttpException;

import javax.print.attribute.standard.Media;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.time.LocalDateTime;
import java.util.Optional;

/*
    Catch all Server Exceptions propagated back to client caller through HTTP REST calls and returning json object
    representation of exception message.
 */
@Provider
public class IndyRestMapperExceptionHandler implements ExceptionMapper<Exception> {


    @Override
    public Response toResponse(Exception exception) {
        IndyRestMapperResponse irmpr = new IndyRestMapperResponse(exception.getMessage(), LocalDateTime.now());
        Optional
          .ofNullable(exception.getCause())
          .ifPresent((exc) -> { irmpr.setCause(exc.getCause()); } );
        return Response.serverError().entity(irmpr).type(MediaType.APPLICATION_JSON).build();
    }
}
