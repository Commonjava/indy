package org.commonjava.aprox.spi;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.commonjava.aprox.dto.UIRoute;
import org.commonjava.aprox.dto.UISection;
import org.commonjava.aprox.model.io.AproxObjectMapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AproxAddOnIDTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final ObjectMapper mapper = new AproxObjectMapper( true );

    @Test
    public void roundTripSerialize()
        throws Exception
    {
        final UIRoute route = new UIRoute( "/foo", "/partials/foo.html" );
        final UISection section = new UISection( "Foo (Add-On)", route.getRoute() );

        final AproxAddOnID id = new AproxAddOnID().withName( "foo" )
                                                  .withInitJavascriptHref( "js/foo.js" )
                                                  .withRoute( route )
                                                  .withSection( section );

        final String json = mapper.writeValueAsString( id );

        logger.info( json );

        final AproxAddOnID result = mapper.readValue( json, AproxAddOnID.class );

        assertThat( result.getName(), equalTo( id.getName() ) );
        assertThat( result.getInitJavascriptHref(), equalTo( id.getInitJavascriptHref() ) );

        final List<UIRoute> rRoutes = result.getRoutes();
        assertThat( rRoutes, notNullValue() );
        assertThat( rRoutes.size(), equalTo( 1 ) );

        final UIRoute rRoute = rRoutes.get( 0 );
        assertThat( rRoute.getTemplateHref(), equalTo( route.getTemplateHref() ) );
        assertThat( rRoute.getRoute(), equalTo( route.getRoute() ) );

        final List<UISection> rSections = result.getSections();
        assertThat( rSections, notNullValue() );
        assertThat( rSections.size(), equalTo( 1 ) );

        final UISection rSection = rSections.get( 0 );
        assertThat( rSection.getName(), equalTo( section.getName() ) );
        assertThat( rSection.getRoute(), equalTo( section.getRoute() ) );
    }

}
