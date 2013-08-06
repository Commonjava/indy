package org.commonjava.aprox.depgraph.json;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.EGraphManager;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public class EProjectWebSer
    extends AbstractProjectNetSer<EProjectWeb>
{

    private final EGraphManager graphs;

    private final GraphWorkspaceHolder sessionManager;

    public EProjectWebSer( final EGraphManager graphs, final GraphWorkspaceHolder sessionManager )
    {
        this.graphs = graphs;
        this.sessionManager = sessionManager;
    }

    @Override
    public EProjectWeb deserialize( final JsonElement src, final Type typeInfo, final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        final JsonObject obj = src.getAsJsonObject();

        final Set<ProjectVersionRef> roots = new HashSet<ProjectVersionRef>();

        final JsonElement rootsElem = obj.get( SerializationConstants.WEB_ROOTS );
        if ( rootsElem != null )
        {
            final JsonArray rootsArry = rootsElem.getAsJsonArray();
            for ( final JsonElement e : rootsArry )
            {
                final ProjectVersionRef ref = ctx.deserialize( e, ProjectVersionRef.class );
                if ( ref != null )
                {
                    roots.add( ref );
                }
            }
        }

        final Collection<ProjectRelationship<?>> rels = deserializeRelationships( obj, ctx );
        final Set<EProjectCycle> cycles = deserializeCycles( obj, ctx );

        graphs.storeRelationships( rels );

        final EProjectWeb web = graphs.getWeb( sessionManager.getCurrentWorkspace(), roots );

        for ( final EProjectCycle cycle : cycles )
        {
            web.addCycle( cycle );
        }

        return web;
    }

    @Override
    public JsonElement serialize( final EProjectWeb src, final Type typeInfo, final JsonSerializationContext ctx )
    {
        final JsonObject obj = new JsonObject();

        final JsonArray rootArry = new JsonArray();
        final Set<ProjectVersionRef> roots = src.getRoots();
        for ( final ProjectVersionRef root : roots )
        {
            rootArry.add( ctx.serialize( root ) );
        }

        if ( rootArry.size() > 0 )
        {
            obj.add( SerializationConstants.WEB_ROOTS, rootArry );
        }

        serializeRelationships( src, obj, ctx );
        serializeCycles( src, obj, ctx );

        return obj;
    }

}
