package org.commonjava.indy.ftest.core.jaxrs;

import groovy.json.JsonBuilder;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.swagger.util.Json;
import io.undertow.util.StatusCodes;
import org.apache.http.HttpStatus;
import org.apache.http.protocol.HttpService;
import org.commonjava.indy.IndyException;
import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.rest.IndyRestMapperResponse;
import org.jboss.resteasy.client.exception.ResteasyHttpException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.Throwables;

import javax.validation.constraints.AssertTrue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.io.IOException;

import static org.hamcrest.core.Is.is;

public class IndyRestMapperExceptionHandlerTest extends AbstractIndyFunctionalTest {


    @Test
    public void restTest() {

        try {

            IndyClientHttp indyClient1 = client.module(IndyRawHttpModule.class).getHttp();
            Response response1 = indyClient1.get("/test/exc1", Response.class);
            Assert.assertNotNull(response1);
            Assert.assertThat("Test Exception Message", is(response1.readEntity(IndyRestMapperResponse.class).getMessage()));
            Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, is(response1.getStatus()));


            IndyClientHttp indyClient2 = client.module(IndyRawHttpModule.class).getHttp();
            Response response2 = indyClient2.get("/test/exc2", Response.class);
            Assert.assertNotNull(response2);
            Assert.assertThat(HttpStatus.SC_BAD_REQUEST,is(response2.getStatus()));

            IndyClientHttp indyClient3 = client.module(IndyRawHttpModule.class).getHttp();
            Response response3 = indyClient3.get("/test/exc3", Response.class);
            Assert.assertNotNull(response3);
            Assert.assertThat("WebApplicationException Message", is(response3.readEntity(IndyRestMapperResponse.class).getMessage()));
            Assert.assertThat(HttpStatus.SC_INTERNAL_SERVER_ERROR, is(response3.getStatusInfo().getStatusCode()));

            IndyClientHttp indyClient4 = client.module(IndyRawHttpModule.class).getHttp();
            Response response4 = indyClient4.get("/test/exc4", Response.class);
            Assert.assertNotNull(response4);
            Assert.assertThat("ResteasyHttpException Message", is(response4.readEntity(IndyRestMapperResponse.class).getMessage()));
            Assert.assertThat(HttpStatus.SC_INTERNAL_SERVER_ERROR, is(response4.getStatusInfo().getStatusCode()));


            IndyClientHttp indyClient7 = client.module(IndyRawHttpModule.class).getHttp();
            Response response7 = indyClient7.get("/test/exc7", Response.class);
            Assert.assertNotNull(response7);
            Assert.assertThat("Test IOException", is(response7.readEntity(IndyRestMapperResponse.class).getCause()));
            Assert.assertThat(HttpStatus.SC_SERVICE_UNAVAILABLE, is(response7.getStatusInfo().getStatusCode()));


        } catch (IndyClientException e) {
            e.printStackTrace();
        }


    }

    @Path("/test")
    class TestExcpetionResource {

        @GET
        @Path("/exc1")
        public Response exc1() {
            throw new RuntimeException("Test Exception Message");
        }

        @GET
        @Path("/exc2")
        public Response exc2() throws IndyWorkflowException {
            throw new IndyWorkflowException("Test Exception Message",null);
        }

        @GET
        @Path("/exc3")
        public Response exc3() {
            throw new WebApplicationException("WebApplicationException Message", StatusCodes.INTERNAL_SERVER_ERROR);
        }

        @GET
        @Path("/exc4")
        public Response exc4() {
            throw new ResteasyHttpException("ResteasyHttpException Message");
        }


        @GET
        @Path("/exc7")
        public Response exc7() throws IOException {
            throw new IOException("Test IOException");
        }

    }
}
