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
package org.commonjava.indy.subsys.cpool;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class CPInitialContextFactory
        implements InitialContextFactory
{
    private static CPInitialContext context;

    @Override
    public synchronized Context getInitialContext( final Hashtable<?, ?> hashtable )
            throws NamingException
    {
        if ( context == null )
        {
            context = new CPInitialContext();
        }

        return context;
    }

    public static final class CPInitialContext extends  InitialContext
    {

        private Map<String, Object> bindings = new HashMap<>();

        public CPInitialContext()
                throws NamingException
        {
        }

        public CPInitialContext( final Hashtable<?, ?> environment )
                throws NamingException
        {
        }

        @Override
        protected void init( final Hashtable<?, ?> environment )
                throws NamingException
        {
        }

        @Override
        public Context createSubcontext( final Name name )
                throws NamingException
        {
            return this;
        }

        @Override
        public Context createSubcontext( final String name )
                throws NamingException
        {
            return this;
        }

        @Override
        protected Context getDefaultInitCtx()
                throws NamingException
        {
            return this;
        }

        @Override
        public void bind( final Name name, final Object obj )
                throws NamingException
        {
            bindings.put( toString( name ), obj );
        }

        @Override
        public Object lookup( final String name )
                throws NamingException
        {
            return bindings.get( name );
        }

        @Override
        public Object lookup( final Name name )
                throws NamingException
        {
            return bindings.get( toString( name ) );
        }

        @Override
        public void bind( final String name, final Object obj )
                throws NamingException
        {
            bindings.put( name, obj );
        }

        @Override
        public void rebind( final String name, final Object obj )
                throws NamingException
        {
            bindings.put( name, obj );
        }

        @Override
        public void rebind( final Name name, final Object obj )
                throws NamingException
        {
            bindings.put( toString( name ), obj );
        }

        private String toString( Name name )
        {
            StringBuilder sb = new StringBuilder();
            for( Enumeration<String> e = name.getAll(); e.hasMoreElements(); )
            {
                String part = e.nextElement();
                if ( sb.length() > 0 )
                {
                    sb.append( '/' );
                }
                sb.append( part );
            }

            return sb.toString();
        }
    }
}
