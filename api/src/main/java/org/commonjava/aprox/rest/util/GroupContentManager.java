package org.commonjava.aprox.rest.util;

import java.io.InputStream;

import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.rest.AproxWorkflowException;

public interface GroupContentManager
{

    StorageItem retrieve( String name, String path )
        throws AproxWorkflowException;

    StorageItem store( String name, String path, InputStream stream )
        throws AproxWorkflowException;

}