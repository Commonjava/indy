package org.commonjava.aprox.tensor.data;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import org.apache.maven.graph.common.ref.ProjectVersionRef;
import org.apache.maven.graph.effective.ref.EProjectKey;
import org.commonjava.tensor.data.TensorDataManager;
import org.commonjava.tensor.event.ProjectRelationshipsErrorEvent;

@ApplicationScoped
public class AproxTensorDataManager
{

    private static final String APROX_TENSOR_MODEL_ERRORS = "aproxTensor-modelErrors";

    private static final String ERROR_SEPARATOR = Pattern.quote( "_::--::_" );

    @Inject
    private TensorDataManager dataManager;

    @Inject
    private Event<ProjectRelationshipsErrorEvent> event;

    public AproxTensorDataManager()
    {
    }

    public AproxTensorDataManager( final TensorDataManager dataManager )
    {
        this.dataManager = dataManager;
    }

    public boolean hasErrors( final String g, final String a, final String v )
    {
        final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );
        final Map<String, String> md = dataManager.getMetadata( new EProjectKey( ref ) );
        if ( md == null )
        {
            return false;
        }

        return md.containsKey( APROX_TENSOR_MODEL_ERRORS );
    }

    public Set<String> getErrors( final String g, final String a, final String v )
    {
        final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );
        final EProjectKey key = new EProjectKey( ref );

        final Map<String, String> md = dataManager.getMetadata( key );
        if ( md == null )
        {
            return null;
        }

        final String serialized = md.get( APROX_TENSOR_MODEL_ERRORS );

        if ( isEmpty( serialized ) )
        {
            return null;
        }

        final String[] errors = serialized.split( ERROR_SEPARATOR );
        return new HashSet<String>( Arrays.asList( errors ) );
    }

    public synchronized void addError( final String g, final String a, final String v, final Throwable error )
    {
        final ProjectVersionRef ref = new ProjectVersionRef( g, a, v );
        final EProjectKey key = new EProjectKey( ref );

        final Map<String, String> md = dataManager.getMetadata( key );
        if ( md == null )
        {
            return;
        }

        String serialized = md.get( APROX_TENSOR_MODEL_ERRORS );

        final String errorStr = toString( error );
        if ( isEmpty( serialized ) )
        {
            serialized = errorStr;
        }
        else
        {
            serialized += ERROR_SEPARATOR + errorStr;
        }

        dataManager.addMetadata( key, APROX_TENSOR_MODEL_ERRORS, serialized );
    }

    private String toString( final Throwable e )
    {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter( sw );

        e.printStackTrace( pw );

        return sw.toString();
    }

}
