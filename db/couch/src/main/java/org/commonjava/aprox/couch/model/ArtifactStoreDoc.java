package org.commonjava.aprox.couch.model;

import org.commonjava.aprox.model.ArtifactStore;
import org.commonjava.couch.model.CouchDocument;

public interface ArtifactStoreDoc<T extends ArtifactStore>
    extends CouchDocument
{
    T exportStore();

}
