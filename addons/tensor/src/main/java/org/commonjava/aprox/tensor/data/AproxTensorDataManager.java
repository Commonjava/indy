package org.commonjava.aprox.tensor.data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.data.store.IndexStore;
import org.commonjava.tensor.data.store.IndexStoreFactory;
import org.commonjava.tensor.event.ErrorKey;
import org.commonjava.tensor.event.ProjectRelationshipsErrorEvent;
import org.commonjava.util.logging.Logger;

import com.google.gson.reflect.TypeToken;

@ApplicationScoped
public class AproxTensorDataManager
{

    private static final String APROX_TENSOR_MODEL_ERRORS = "aproxTensor-modelErrors";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private IndexStoreFactory indexFactory;

    @Inject
    private Event<ProjectRelationshipsErrorEvent> event;

    private IndexStore<ErrorKey, Set<String>> errors;

    public AproxTensorDataManager()
    {
    }

    public AproxTensorDataManager( final IndexStore<ErrorKey, Set<String>> errors )
    {
        this.errors = errors;
    }

    @PostConstruct
    public void initialize()
    {
        errors = indexFactory.getStore( APROX_TENSOR_MODEL_ERRORS, ErrorKey.class, new TypeToken<Set<String>>()
        {
        } );
    }

    public boolean hasErrors( final String g, final String a, final String v )
    {
        return errors.contains( new ErrorKey( g, a, v ) );
    }

    public Set<String> getErrors( final String g, final String a, final String v )
    {
        final ErrorKey key = new ErrorKey( g, a, v );
        try
        {
            return errors.get( key );
        }
        catch ( final TensorDataException e )
        {
            logger.error( "Failed to retrieve errors for: %s. Reason: %s", e, key, e.getMessage() );
            throw new IllegalStateException( "Tensor error store is not functioning: " + e.getMessage(), e );
        }
    }

    public synchronized void addError( final String g, final String a, final String v, final Throwable error )
    {
        final ErrorKey key = new ErrorKey( g, a, v );
        try
        {
            Set<String> errors = this.errors.get( key );
            if ( errors == null )
            {
                errors = new HashSet<String>();
            }

            errors.add( toString( error ) );
            this.errors.store( key, errors );

            if ( event != null )
            {
                event.fire( new ProjectRelationshipsErrorEvent( key, error ) );
            }
        }
        catch ( final TensorDataException e )
        {
            logger.error( "Failed to save errors for: %s. Reason: %s", e, key, e.getMessage() );
            throw new IllegalStateException( "Tensor error store is not functioning: " + e.getMessage(), e );
        }
    }

    private String toString( final Throwable e )
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter( sw );

        e.printStackTrace( pw );

        return sw.toString();
    }

}
