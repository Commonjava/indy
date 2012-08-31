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
package org.commonjava.aprox.couch.model;

import org.commonjava.aprox.model.Repository;
import org.commonjava.couch.model.DenormalizedCouchDoc;

public class RepositoryDoc
    extends AbstractArtifactStoreDoc<Repository>
    implements DenormalizedCouchDoc
{

    public RepositoryDoc( final String id, final String rev, final String modelVersion, final Repository repo )
    {
        super( id, rev, modelVersion, repo );
    }

    public RepositoryDoc( final Repository repo )
    {
        super( repo );
    }
}
