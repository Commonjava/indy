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
package org.commonjava.indy.subsys.http.util;

import org.commonjava.maven.galley.auth.PasswordEntry;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.auth.PasswordManager;
import org.commonjava.maven.galley.transport.htcli.internal.util.LocationLookup;
import org.commonjava.util.jhttpc.auth.PasswordKey;
import org.commonjava.util.jhttpc.auth.PasswordType;
import org.commonjava.util.jhttpc.model.SiteConfig;

/**
 * Created by jdcasey on 11/5/15.
 */
public class HttpFactoryPasswordDelegate
        implements org.commonjava.util.jhttpc.auth.PasswordManager
{
    private PasswordManager delegate;

    private LocationLookup locationLookup;

    public HttpFactoryPasswordDelegate( PasswordManager delegate, LocationLookup locationLookup )
    {
        this.delegate = delegate;
        this.locationLookup = locationLookup;
    }

    @Override
    public void bind( String password, SiteConfig config, PasswordType type )
    {
        throw new RuntimeException( "Read-Only password-manager delegate for: " + delegate.getClass().getName() );
    }

    @Override
    public void bind( String password, String siteId, PasswordType type )
    {
        throw new RuntimeException( "Read-Only password-manager delegate for: " + delegate.getClass().getName() );
    }

    @Override
    public void bind( String password, PasswordKey id )
    {
        throw new RuntimeException( "Read-Only password-manager delegate for: " + delegate.getClass().getName() );
    }

    @Override
    public void unbind( SiteConfig config, PasswordType type )
    {
        throw new RuntimeException( "Read-Only password-manager delegate for: " + delegate.getClass().getName() );
    }

    @Override
    public void unbind( String siteId, PasswordType type )
    {
        throw new RuntimeException( "Read-Only password-manager delegate for: " + delegate.getClass().getName() );
    }

    @Override
    public void unbind( PasswordKey id )
    {
        throw new RuntimeException( "Read-Only password-manager delegate for: " + delegate.getClass().getName() );
    }

    @Override
    public String lookup( PasswordKey id )
    {
        Location location = locationLookup.lookup( id.getSiteId() );
        return location == null ?
                null :
                delegate.getPassword( new PasswordEntry( location, id.getPasswordType().name() ) );
    }
}
