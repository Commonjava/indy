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
package org.commonjava.aprox.stats;

import org.commonjava.web.json.ser.JsonSerializer;
import org.junit.Test;

public class AProxVersioningTest
{

    @Test
    public void serializeToJson()
    {
        final String json = new JsonSerializer().toString( new AProxVersioning() );

        System.out.println( json );
    }

}
