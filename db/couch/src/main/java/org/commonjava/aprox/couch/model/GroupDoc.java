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

import org.commonjava.aprox.model.Group;

public class GroupDoc
    extends AbstractArtifactStoreDoc<Group>
{

    public GroupDoc( final Group group )
    {
        super( group );
    }

    public GroupDoc( final String id, final String rev, final String modelVersion, final Group group )
    {
        super( id, rev, modelVersion, group );
    }

}
