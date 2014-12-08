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
package org.commonjava.aprox.depgraph.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.commonjava.aprox.model.core.StoreKey;
import org.commonjava.aprox.model.core.StoreType;
import org.junit.Test;

public class AproxDiscoveryUtilsTest
{

    @Test
    public void parseTypeAndNameFromAproxURI()
        throws Exception
    {
        final URI uri = new URI( "aprox:group:test" );
        final StoreKey key = AproxDepgraphUtils.getDiscoveryStore( uri );

        assertThat( key, notNullValue() );
        assertThat( key.getType(), equalTo( StoreType.group ) );
        assertThat( key.getName(), equalTo( "test" ) );
    }

}
