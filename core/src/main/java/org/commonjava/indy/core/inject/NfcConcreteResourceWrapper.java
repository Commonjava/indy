package org.commonjava.indy.core.inject;

import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;

import java.io.Serializable;

/**
 * Created by ruhan on 11/29/17.
 */
@Indexed
public class NfcConcreteResourceWrapper implements Serializable
{
    private static final long serialVersionUID = 1L;

    @Field( index = Index.YES, analyze = Analyze.NO )
    private String location;

    @Field ( index = Index.YES, analyze = Analyze.NO )
    private String path;

    public NfcConcreteResourceWrapper( ConcreteResource resource )
    {
        this.location = ( (KeyedLocation) resource.getLocation() ).getKey().toString();
        this.path = resource.getPath();
    }

    public String getLocation()
    {
        return location;
    }

    public String getPath()
    {
        return path;
    }

}