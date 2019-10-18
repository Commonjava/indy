package org.commonjava.indy.ftest.core.jaxrs;

import groovy.json.JsonBuilder;
import io.netty.handler.codec.json.JsonObjectDecoder;
import io.swagger.util.Json;
import io.undertow.util.StatusCodes;
import org.apache.http.HttpStatus;
import org.commonjava.indy.IndyException;
import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientHttp;
import org.commonjava.indy.client.core.module.IndyRawHttpModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.model.rest.IndyRestMapperResponse;
import org.jboss.resteasy.client.exception.ResteasyHttpException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.Throwables;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static org.hamcrest.core.Is.is;

public class IndyRestMapperExceptionHandlerTest extends AbstractIndyFunctionalTest {


    @Test
    public void restTest() {

        try {

            IndyClientHttp indyClient1 = client.module(IndyRawHttpModule.class).getHttp();
            Response response1 = indyClient1.get("/test/exc1", Response.class);
            Assert.assertNotNull(response1);
            Assert.assertThat("Test Exception Message", is(response1.readEntity(IndyRestMapperResponse.class).getMessage()));

            IndyClientHttp indyClient2 = client.module(IndyRawHttpModule.class).getHttp();
            Response response2 = indyClient2.get("/test/exc2", Response.class);
            Assert.assertNotNull(response2);
            Assert.assertNull(response2.readEntity(IndyRestMapperResponse.class).getMessage());

            IndyClientHttp indyClient3 = client.module(IndyRawHttpModule.class).getHttp();
            Response response3 = indyClient3.get("/test/exc3", Response.class);
            Assert.assertNotNull(response3);
            Assert.assertThat("WebApplicationException Message", is(response1.readEntity(IndyRestMapperResponse.class).getMessage()));
            Assert.assertThat(HttpStatus.SC_OK, is(response3.getStatusInfo().getStatusCode()));

            IndyClientHttp indyClient4 = client.module(IndyRawHttpModule.class).getHttp();
            Response response4 = indyClient4.get("/test/exc4", Response.class);
            Assert.assertNotNull(response4);
            Assert.assertThat("ResteasyHttpException Message", is(response1.readEntity(IndyRestMapperResponse.class).getMessage()));
            Assert.assertThat(HttpStatus.SC_OK, is(response4.getStatusInfo().getStatusCode()));

            IndyClientHttp indyClient5 = client.module(IndyRawHttpModule.class).getHttp();
            Response response5 = indyClient5.get("/test/exc5", Response.class);
            Assert.assertNotNull(response5);
            Assert.assertThat("Exception Message", is(response1.readEntity(IndyRestMapperResponse.class).getMessage()));
            Assert.assertThat("Throwable Cause", is(response1.readEntity(IndyRestMapperResponse.class).getCause()));
            Assert.assertThat(HttpStatus.SC_OK, is(response5.getStatusInfo().getStatusCode()));

            IndyClientHttp indyClient6 = client.module(IndyRawHttpModule.class).getHttp();
            Response response6 = indyClient6.get("/test/exc6", Response.class);
            Assert.assertNotNull(response6);
            Assert.assertThat("Test Throwable", is(response1.readEntity(IndyRestMapperResponse.class).getCause()));
            Assert.assertThat("Throwable Cause", is(response1.readEntity(IndyRestMapperResponse.class).getCause()));
            Assert.assertThat(HttpStatus.SC_OK, is(response6.getStatusInfo().getStatusCode()));


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
        public Response exc2() throws IndyException {
            throw new IndyException();
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
        @Path("/exc5")
        public Response exc5() throws Exception {
            throw new Exception("Exception Message", new Throwable("Throwable Cause"));
        }

        @GET
        @Path("/exc6")
        public Response exc6() throws Throwable {
            throw new Throwable("Test Throwable", new Throwable("Throwable Cause"));
        }

    }
}
