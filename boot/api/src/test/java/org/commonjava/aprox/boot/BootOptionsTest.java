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
package org.commonjava.aprox.boot;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.commonjava.aprox.boot.BootOptions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BootOptionsTest
{

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void replaceAproxHomeInValue()
        throws InterpolationException, IOException
    {
        final File bootProps = temp.newFile( "boot.properties" );
        FileUtils.writeStringToFile( bootProps, "" );

        final BootOptions opts = new BootOptions( bootProps, "/path/to/aprox" );

        final String val = opts.resolve( "${aprox.home}/etc/aprox/main.conf" );

        assertThat( val, equalTo( "/path/to/aprox/etc/aprox/main.conf" ) );
    }

    @Test
    public void replaceOtherPropReferencingAproxHomeInValue()
        throws InterpolationException, IOException
    {
        final File bootProps = temp.newFile( "boot.properties" );
        FileUtils.writeStringToFile( bootProps, "myDir = ${aprox.home}/custom" );

        final BootOptions opts = new BootOptions( bootProps, "/path/to/aprox" );

        final String val = opts.resolve( "${myDir}/etc/aprox/main.conf" );

        assertThat( val, equalTo( "/path/to/aprox/custom/etc/aprox/main.conf" ) );
    }

}
