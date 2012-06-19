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

import org.commonjava.aprox.core.model.ArtifactStore;
import org.commonjava.couch.model.AbstractCouchDocument;
import org.commonjava.couch.model.DenormalizedCouchDoc;

public abstract class AbstractArtifactStoreDoc<T extends ArtifactStore>
    extends AbstractCouchDocument
    implements DenormalizedCouchDoc, ArtifactStoreDoc<T>
{

    public static final String COUCH_DOC_ID = "couch-doc-id";

    public static final String COUCH_DOC_REV = "couch-doc-rev";

    private final T store;

    protected AbstractArtifactStoreDoc( final T store )
    {
        this.store = store;
        calculateDenormalizedFields();
    }

    @Override
    public T exportStore()
    {
        store.setMetadata( COUCH_DOC_ID, getCouchDocId() );
        store.setMetadata( COUCH_DOC_REV, getCouchDocRev() );

        return store;
    }

    @Override
    public void calculateDenormalizedFields()
    {
        final String id = store.getMetadata( COUCH_DOC_ID );
        if ( id != null )
        {
            setCouchDocId( id );
        }
        else
        {
            setCouchDocId( store.getKey()
                                .toString() );
        }

        final String rev = store.getMetadata( COUCH_DOC_REV );
        if ( rev != null )
        {
            setCouchDocRev( rev );
        }
    }

}
