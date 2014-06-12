package org.commonjava.aprox.autoprox.rest;

import java.net.MalformedURLException;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.autoprox.data.AutoProxCatalog;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.model.HostedRepository;
import org.commonjava.aprox.model.RemoteRepository;
import org.commonjava.aprox.util.ApplicationStatus;

@ApplicationScoped
public class AutoProxWorksheetController
{

    @Inject
    private AutoProxCatalog catalog;

    public RemoteRepository evalRemoteRepositoryUrl( final String name )
        throws AproxWorkflowException
    {
        try
        {
            return catalog.createRemoteRepository( name );
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR,
                                              "Invalid URL resulted from evaluating: '{}'. Reason: {}", e, name,
                                              e.getMessage() );
        }
    }

    public HostedRepository evalHostedRepository( final String name )
    {
        return catalog.createHostedRepository( name );
    }

    public Group evalGroup( final String name )
    {
        return catalog.createGroup( name );
    }

    public RemoteRepository evalGroupValidationRepo( final String name )
        throws AproxWorkflowException
    {
        try
        {
            return catalog.createGroupValidationRemote( name );
        }
        catch ( final MalformedURLException e )
        {
            throw new AproxWorkflowException( ApplicationStatus.SERVER_ERROR,
                                              "Invalid URL resulted from evaluating: '{}'. Reason: {}", e, name,
                                              e.getMessage() );
        }
    }

    public String evalRepositoryValidationPath( final String name )
    {
        return catalog.getRemoteValidationUrl( name );
    }

}
