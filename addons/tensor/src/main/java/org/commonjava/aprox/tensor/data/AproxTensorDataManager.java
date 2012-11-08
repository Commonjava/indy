package org.commonjava.aprox.tensor.data;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;

@ApplicationScoped
public class AproxTensorDataManager
{

    private static final String APROX_TENSOR_MODEL_ERRORS = "aproxTensor-modelErrors";

    @Inject
    private CacheContainer container;

    private Cache<String, Set<Throwable>> errors;

    public AproxTensorDataManager()
    {
    }

    public AproxTensorDataManager( final Cache<String, Set<Throwable>> errors )
    {
        this.errors = errors;
    }

    @PostConstruct
    public void initialize()
    {
        errors = container.getCache( APROX_TENSOR_MODEL_ERRORS );
        errors.start();
    }

    public boolean hasErrors( final String projectId )
    {
        return errors.containsKey( projectId );
    }

    public Set<Throwable> getErrors( final String projectId )
    {
        return errors.get( projectId );
    }

    public synchronized void addError( final String projectId, final Throwable e )
    {
        Set<Throwable> errors = this.errors.get( projectId );
        if ( errors == null )
        {
            errors = new HashSet<Throwable>();
            this.errors.put( projectId, errors );
        }

        errors.add( e );
    }

}
