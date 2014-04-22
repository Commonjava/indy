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
package org.commonjava.aprox.dotmaven.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class NameUtilsTest
{

    @Test
    public void checkInvalidURI()
    {
        assertThat( NameUtils.isValidResource( "/.DS_Store" ), equalTo( false ) );
    }

    @Test
    public void checkInvalidURILeaf()
    {
        assertThat( NameUtils.isValidResource( "/path/to/.DS_Store" ), equalTo( false ) );
    }

}
