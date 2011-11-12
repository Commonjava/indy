package org.commonjava.aprox.core.model;

import org.commonjava.couch.model.CouchDocument;

public interface ArtifactStore
    extends CouchDocument
{

    StoreKey getKey();

    String getName();

    StoreType getDoctype();

}