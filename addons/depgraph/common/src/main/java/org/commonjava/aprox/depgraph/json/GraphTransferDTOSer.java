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
package org.commonjava.aprox.depgraph.json;

import static org.commonjava.aprox.depgraph.json.JsonUtils.deserializeCycles;
import static org.commonjava.aprox.depgraph.json.JsonUtils.deserializeRelationships;
import static org.commonjava.aprox.depgraph.json.JsonUtils.serializeCycles;
import static org.commonjava.aprox.depgraph.json.JsonUtils.serializeRelationships;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.aprox.depgraph.dto.GraphTransferDTO;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class GraphTransferDTOSer
    implements JsonSerializer<GraphTransferDTO>, JsonDeserializer<GraphTransferDTO>
{

    @Override
    public GraphTransferDTO deserialize( final JsonElement src, final Type typeInfo, final JsonDeserializationContext ctx )
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

        final Set<ProjectRelationship<?>> rels = deserializeRelationships( obj, ctx );
        final Set<EProjectCycle> cycles = deserializeCycles( obj, ctx );

        return new GraphTransferDTO( roots, rels, cycles );
    }

    @Override
    public JsonElement serialize( final GraphTransferDTO src, final Type typeInfo, final JsonSerializationContext ctx )
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

        serializeRelationships( src.getRelationships(), obj, ctx );
        serializeCycles( src.getCycles(), obj, ctx );

        return obj;
    }

}
