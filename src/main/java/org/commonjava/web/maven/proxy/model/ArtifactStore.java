package org.commonjava.web.maven.proxy.model;

import org.commonjava.couch.model.CouchDocument;

public interface ArtifactStore
    extends CouchDocument
{

    StoreKey getKey();

    String getName();

    StoreType getDoctype();

}