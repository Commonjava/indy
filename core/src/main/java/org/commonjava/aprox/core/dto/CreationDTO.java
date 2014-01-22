package org.commonjava.aprox.core.dto;

import java.net.URI;

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
