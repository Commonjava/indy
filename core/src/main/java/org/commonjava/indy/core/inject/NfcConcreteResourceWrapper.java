package org.commonjava.indy.core.inject;

import org.commonjava.indy.model.galley.KeyedLocation;
import org.commonjava.maven.galley.model.ConcreteResource;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;

/**
 * Created by ruhan on 11/29/17.
 */
@Indexed
public class NfcConcreteResourceWrapper
{
    @Field( index = Index.YES, analyze = Analyze.NO )
    private String location;

    @Field ( index = Index.YES )
    private String path;

    @Field
    private long timeout;

    public NfcConcreteResourceWrapper( ConcreteResource resource, long timeout )
    {
        this.location = ( (KeyedLocation) resource.getLocation() ).getKey().toString();
        this.path = resource.getPath();
        this.timeout = timeout;
    }

    public String getLocation()
    {
        return location;
    }

    public String getPath()
    {
        return path;
    }

    public long getTimeout()
    {
        return timeout;
    }
}