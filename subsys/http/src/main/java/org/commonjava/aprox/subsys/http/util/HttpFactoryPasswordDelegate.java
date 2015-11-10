package org.commonjava.aprox.subsys.http.util;

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
