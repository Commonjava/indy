/*******************************************************************************
 * Copyright (C) 2014 John Casey.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.depgraph.json;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.cartographer.data.CartoDataException;
import org.commonjava.maven.cartographer.data.CartoDataManager;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;

public class EProjectWebSer
    extends AbstractProjectNetSer<EProjectWeb>
{

    private final CartoDataManager data;

    public EProjectWebSer( final CartoDataManager data )
    {
        this.data = data;
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

        final EProjectWeb web;
        try
        {
            data.storeRelationships( rels );
            web = data.getProjectWeb( roots.toArray( new ProjectVersionRef[roots.size()] ) );
        }
        catch ( final CartoDataException e )
        {
            throw new JsonParseException( "Failed to store relationships or retrieve resulting project web.", e );
        }

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
