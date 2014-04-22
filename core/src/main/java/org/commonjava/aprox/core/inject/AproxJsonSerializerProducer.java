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
package org.commonjava.aprox.core.inject;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.commonjava.aprox.inject.AproxData;
import org.commonjava.aprox.model.io.StoreKeySerializer;
import org.commonjava.web.json.ser.JsonSerializer;

@Singleton
public class AproxJsonSerializerProducer
{

    private JsonSerializer serializer;

    @Produces
    @AproxData
    @Default
    public synchronized JsonSerializer getSerializer()
    {
        if ( serializer == null )
        {
            serializer = new JsonSerializer( new StoreKeySerializer() );
        }

        return serializer;
    }

}
