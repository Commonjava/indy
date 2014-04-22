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
package org.commonjava.aprox.subsys.infinispan.io;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Set;

import org.commonjava.util.logging.Logger;
import org.commonjava.web.json.ser.JsonSerializer;
import org.infinispan.marshall.AbstractExternalizer;

public abstract class JsonExternalizer<T>
    extends AbstractExternalizer<T>
{

    private static final long serialVersionUID = 1L;

    private final JsonSerializer serializer;

    private final Class<T> type;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final Charset encoding;

    protected JsonExternalizer( final Class<T> type, final JsonSerializer serializer )
    {
        this.encoding = Charset.forName( "UTF-8" );
        this.type = type;
        this.serializer = serializer;
    }

    @Override
    public Set<Class<? extends T>> getTypeClasses()
    {
        return Collections.<Class<? extends T>> singleton( type );
    }

    @Override
    public void writeObject( final ObjectOutput output, final T object )
        throws IOException
    {
        final String json = new String( serializer.toString( object )
                                                  .getBytes( encoding ) );

        logger.debug( "Serializing JSON for type: {}\n\n{}\n", type.getName(), json );
        output.writeObject( json );
    }

    @SuppressWarnings( "unchecked" )
    @Override
    public T readObject( final ObjectInput input )
        throws IOException, ClassNotFoundException
    {
        final String raw = (String) input.readObject();
        final String encoded = new String( raw.getBytes( encoding ) );
        logger.debug( "Deserializing JSON for type: {}\n\n{}\n", type.getName(), encoded );

        return serializer.fromString( encoded, type );
    }

}
