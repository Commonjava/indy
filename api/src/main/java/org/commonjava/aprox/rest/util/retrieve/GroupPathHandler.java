/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.rest.util.retrieve;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.commonjava.aprox.io.StorageItem;
import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.aprox.model.Group;
import org.commonjava.aprox.rest.AproxWorkflowException;

public interface GroupPathHandler
{

    String MERGEINFO_SUFFIX = ".info";

    String SHA_SUFFIX = ".sha";

    String MD5_SUFFIX = ".md5";

    boolean canHandle( String path );

    StorageItem retrieve( Group group, List<? extends ArtifactStore> stores, String path )
        throws AproxWorkflowException;

    StorageItem store( Group group, List<? extends ArtifactStore> stores, String path, InputStream stream )
        throws AproxWorkflowException;

    boolean delete( Group group, List<? extends ArtifactStore> stores, String path )
        throws AproxWorkflowException, IOException;

}
