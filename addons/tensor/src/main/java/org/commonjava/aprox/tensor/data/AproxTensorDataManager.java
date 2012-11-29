package org.commonjava.aprox.tensor.data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.commonjava.tensor.data.TensorDataException;
import org.commonjava.tensor.data.store.IndexStore;
import org.commonjava.tensor.data.store.IndexStoreFactory;
import org.commonjava.util.logging.Logger;

import com.google.gson.reflect.TypeToken;

@ApplicationScoped
public class AproxTensorDataManager
{

    private static final String APROX_TENSOR_MODEL_ERRORS = "aproxTensor-modelErrors";

    private final Logger logger = new Logger( getClass() );

    @Inject
    private IndexStoreFactory indexFactory;

    private IndexStore<String, Set<String>> errors;

    public AproxTensorDataManager()
    {
    }

    public AproxTensorDataManager( final IndexStore<String, Set<String>> errors )
    {
        this.errors = errors;
    }

    @PostConstruct
    public void initialize()
    {
        errors = indexFactory.getStore( APROX_TENSOR_MODEL_ERRORS, new TypeToken<String>()
        {
        }, new TypeToken<Set<String>>()
        {
        } );
    }

    public boolean hasErrors( final String projectId )
    {
        return errors.contains( projectId );
    }

    public Set<String> getErrors( final String projectId )
    {
        try
        {
            return errors.get( projectId );
        }
        catch ( final TensorDataException e )
        {
            logger.error( "Failed to retrieve errors for: %s. Reason: %s", e, projectId, e.getMessage() );
            throw new IllegalStateException( "Tensor error store is not functioning: " + e.getMessage(), e );
        }
    }

    public synchronized void addError( final String projectId, final Throwable error )
    {
        try
        {
            Set<String> errors = this.errors.get( projectId );
            if ( errors == null )
            {
                errors = new HashSet<String>();
            }

            errors.add( toString( error ) );
            this.errors.store( projectId, errors );
        }
        catch ( final TensorDataException e )
        {
            logger.error( "Failed to save errors for: %s. Reason: %s", e, projectId, e.getMessage() );
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
