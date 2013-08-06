package org.commonjava.aprox.rest.util;

import java.io.IOException;
import java.io.InputStream;

import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.galley.model.Transfer;

public interface GroupContentManager
{

    Transfer retrieve( String name, String path )
        throws AproxWorkflowException;

    Transfer store( String name, String path, InputStream stream )
        throws AproxWorkflowException;

    boolean delete( String name, String path )
        throws AproxWorkflowException, IOException;

}