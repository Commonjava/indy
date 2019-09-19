/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.spi;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.model.spi.IndyAddOnID;
import org.commonjava.indy.model.spi.UIRoute;
import org.commonjava.indy.model.spi.UISection;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class IndyAddOnIDTest
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final ObjectMapper mapper = new IndyObjectMapper( true );

    @Test
    public void roundTripSerialize()
        throws Exception
    {
        final UIRoute route = new UIRoute( "/foo", "/partials/foo.html" );
        final UISection section = new UISection( "Foo (Add-On)", route.getRoute() );

        final IndyAddOnID id = new IndyAddOnID().withName( "foo" )
                                                  .withInitJavascriptHref( "js/foo.js" )
                                                  .withRoute( route )
                                                  .withSection( section );

        final String json = mapper.writeValueAsString( id );

        logger.info( json );

        final IndyAddOnID result = mapper.readValue( json, IndyAddOnID.class );

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
