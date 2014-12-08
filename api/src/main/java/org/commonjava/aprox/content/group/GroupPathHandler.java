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
package org.commonjava.aprox.content.group;

import java.io.InputStream;
import java.util.List;

import org.commonjava.aprox.AproxWorkflowException;
import org.commonjava.aprox.model.core.ArtifactStore;
import org.commonjava.aprox.model.core.Group;
import org.commonjava.maven.galley.model.Transfer;

public interface GroupPathHandler
{

    boolean canHandle( String path );

    Transfer retrieve( Group group, List<? extends ArtifactStore> stores, String path )
        throws AproxWorkflowException;

    Transfer store( Group group, List<? extends ArtifactStore> stores, String path, InputStream stream )
        throws AproxWorkflowException;

    boolean delete( Group group, List<? extends ArtifactStore> stores, String path )
        throws AproxWorkflowException;

}
