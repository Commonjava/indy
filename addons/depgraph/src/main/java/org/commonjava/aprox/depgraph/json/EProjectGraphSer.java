package org.commonjava.aprox.depgraph.json;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EProjectGraphSer
    extends AbstractProjectNetSer<EProjectGraph>
    implements JsonSerializer<EProjectGraph>, JsonDeserializer<EProjectGraph>
{

    private final EGraphManager graphs;

    private final GraphWorkspaceHolder sessionManager;

    public EProjectGraphSer( final EGraphManager graphs, final GraphWorkspaceHolder sessionManager )
    {
        this.graphs = graphs;
        this.sessionManager = sessionManager;
    }

    @Override
    public EProjectGraph deserialize( final JsonElement src, final Type typeInfo, final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        final JsonObject obj = src.getAsJsonObject();

        final ProjectVersionRef project =
            ctx.deserialize( obj.get( SerializationConstants.PROJECT_VERSION ), ProjectVersionRef.class );

        final Collection<ProjectRelationship<?>> rels = deserializeRelationships( obj, ctx );

        final Set<EProjectCycle> cycles = deserializeCycles( obj, ctx );

        graphs.storeRelationships( rels );
        final EProjectGraph graph = graphs.getGraph( sessionManager.getCurrentWorkspace(), project );
        if ( graph != null && cycles != null && !cycles.isEmpty() )
        {
            for ( final EProjectCycle cycle : cycles )
            {
                graph.addCycle( cycle );
            }
        }

        return graph;
    }

    @Override
    public JsonElement serialize( final EProjectGraph src, final Type typeInfo, final JsonSerializationContext ctx )
    {
        final JsonObject obj = new JsonObject();
        obj.add( SerializationConstants.PROJECT_VERSION, ctx.serialize( src.getRoot() ) );

        serializeRelationships( src, obj, ctx );
        serializeCycles( src, obj, ctx );

        return obj;
    }

}
