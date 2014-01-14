/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.rest.util;

import java.io.IOException;
import java.io.InputStream;

import org.commonjava.aprox.filer.FileManager;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.galley.model.Transfer;

/**
 * @deprecated Use {@link FileManager} directly instead.
 * @author jdcasey
 */
@Deprecated
public interface GroupContentManager
{

    Transfer retrieve( String name, String path )
        throws AproxWorkflowException;

    Transfer store( String name, String path, InputStream stream )
        throws AproxWorkflowException;

    boolean delete( String name, String path )
        throws AproxWorkflowException, IOException;

}
