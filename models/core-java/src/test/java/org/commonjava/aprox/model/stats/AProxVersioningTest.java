/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.model.stats;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.model.core.io.AproxObjectMapper;
import org.commonjava.aprox.stats.AProxVersioning;
import org.junit.Test;

public class AProxVersioningTest
{

    @Test
    public void roundTripJson()
        throws Exception
    {
        final AproxObjectMapper mapper = new AproxObjectMapper( true );

        final AProxVersioning versioning =
            new AProxVersioning( "0.0.1", "somebody", "01010101010101", "2014-11-02 21:45:00" );

        final String json = mapper.writeValueAsString( versioning );

        System.out.println( json );

        final AProxVersioning result = mapper.readValue( json, AProxVersioning.class );

        assertThat( result.getVersion(), equalTo( versioning.getVersion() ) );
        assertThat( result.getBuilder(), equalTo( versioning.getBuilder() ) );
        assertThat( result.getCommitId(), equalTo( versioning.getCommitId() ) );
        assertThat( result.getTimestamp(), equalTo( versioning.getTimestamp() ) );
    }

}
