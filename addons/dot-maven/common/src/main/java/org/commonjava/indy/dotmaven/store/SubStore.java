/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.dotmaven.store;

import java.io.InputStream;

import net.sf.webdav.StoredObject;
import net.sf.webdav.exceptions.WebdavException;
import net.sf.webdav.spi.ITransaction;

public interface SubStore
{

    /**
     * Return the list of root folders handled by this sub-store.
     * 
     * @return the root folders, NOT null.
     */
    String[] getRootResourceNames();

    /**
     * Determine whether this {@link SubStore} applies to the given resource URI.
     * 
     * @param uri The resource URI to check
     * @return true if this store can service the URI, else false.
     */
    boolean matchesUri( String uri );

    /**
     * Creates a folder at the position specified by <code>folderUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param folderUri
     *      URI of the folder
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void createFolder( ITransaction transaction, String folderUri )
        throws WebdavException;

    /**
     * Creates a content resource at the position specified by
     * <code>resourceUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param resourceUri
     *      URI of the content resource
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void createResource( ITransaction transaction, String resourceUri )
        throws WebdavException;

    /**
     * Gets the content of the resource specified by <code>resourceUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param resourceUri
     *      URI of the content resource
     * @return input stream you can read the content of the resource from
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    InputStream getResourceContent( ITransaction transaction, String resourceUri )
        throws WebdavException;

    /**
     * Sets / stores the content of the resource specified by
     * <code>resourceUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param resourceUri
     *      URI of the resource where the content will be stored
     * @param content
     *      input stream from which the content will be read from
     * @param contentType
     *      content type of the resource or <code>null</code> if unknown
     * @param characterEncoding
     *      character encoding of the resource or <code>null</code> if unknown
     *      or not applicable
     * @return lenght of resource
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    long setResourceContent( ITransaction transaction, String resourceUri, InputStream content, String contentType, String characterEncoding )
        throws WebdavException;

    /**
     * Gets the names of the children of the folder specified by
     * <code>folderUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param folderUri
     *      URI of the folder
     * @return a (possibly empty) list of children, or <code>null</code> if the
     *  uri points to a file
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    String[] getChildrenNames( ITransaction transaction, String folderUri )
        throws WebdavException;

    /**
     * Gets the length of the content resource specified by
     * <code>resourceUri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param resourceUri
     *      URI of the content resource
     * @return length of the resource in bytes, <code>-1</code> declares this
     *  value as invalid and asks the adapter to try to set it from the
     *  properties if possible
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    long getResourceLength( ITransaction transaction, String path )
        throws WebdavException;

    /**
     * Removes the object specified by <code>uri</code>.
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param uri
     *      URI of the object, i.e. content resource or folder
     * @throws WebdavException
     *      if something goes wrong on the store level
     */
    void removeObject( ITransaction transaction, String uri )
        throws WebdavException;

    /**
     * Gets the storedObject specified by <code>uri</code>
     * 
     * @param transaction
     *      indicates that the method is within the scope of a WebDAV
     *      transaction
     * @param uri
     *      URI
     * @return StoredObject
     * @throws WebdavException 
     */
    StoredObject getStoredObject( ITransaction transaction, String uri )
        throws WebdavException;
}
