package org.commonjava.aprox.spi;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.commonjava.aprox.dto.UISection;
import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AproxAddOnIDTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final JsonSerializer serializer = new JsonSerializer();

    @Test
    public void roundTripSerialize()
    {
        final AproxAddOnID id = new AproxAddOnID();
        id.setName( "foo" );
        id.setInitJavascriptHref( "js/foo.js" );

        final UISection section = new UISection( "Foo (Add-On)", "/foo", "/partials/foo.html", "FooMasterController" );
        final List<UISection> sections = new ArrayList<UISection>();
        sections.add( section );

        id.setSections( sections );

        final String json = serializer.toString( id );

        logger.info( json );

        final AproxAddOnID result = serializer.fromString( json, AproxAddOnID.class );

        assertThat( result.getName(), equalTo( id.getName() ) );
        assertThat( result.getInitJavascriptHref(), equalTo( id.getInitJavascriptHref() ) );

        final List<UISection> rSections = result.getSections();
        assertThat( rSections, notNullValue() );
        assertThat( rSections.size(), equalTo( sections.size() ) );

        final UISection rSection = rSections.get( 0 );
        assertThat( rSection.getController(), equalTo( section.getController() ) );
        assertThat( rSection.getName(), equalTo( section.getName() ) );
        assertThat( rSection.getRoute(), equalTo( section.getRoute() ) );
    }

}
