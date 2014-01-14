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
package org.commonjava.aprox.rest.util.retrieve;

import java.io.InputStream;
import java.util.List;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.rest.AproxWorkflowException;
import org.commonjava.maven.galley.model.Transfer;

public interface GroupPathHandler
{

    String MERGEINFO_SUFFIX = ".info";

    String SHA_SUFFIX = ".sha";

    String MD5_SUFFIX = ".md5";

    boolean canHandle( String path );

    Transfer retrieve( Group group, List<? extends ArtifactStore> stores, String path )
        throws AproxWorkflowException;

    Transfer store( Group group, List<? extends ArtifactStore> stores, String path, InputStream stream )
        throws AproxWorkflowException;

    boolean delete( Group group, List<? extends ArtifactStore> stores, String path )
        throws AproxWorkflowException;

}
