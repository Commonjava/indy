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
package org.commonjava.aprox.dto;

import java.net.URI;

/** DTO encapsulating the result of a POST operation somewhere in the system. These should all have similar features, in the sense of having a 
 * <code>Location</code> HTTP header pointing to the new resource, and a JSON response that contains the serialized form of new resource itself.
 */
public class CreationDTO
{

    private final URI uri;

    private final String jsonResponse;

    public CreationDTO( final URI uri )
    {
        this.uri = uri;
        this.jsonResponse = null;
    };

    public CreationDTO( final URI uri, final String jsonResponse )
    {
        this.uri = uri;
        this.jsonResponse = jsonResponse;
    }

    public URI getUri()
    {
        return uri;
    }

    public String getJsonResponse()
    {
        return jsonResponse;
    };

}
