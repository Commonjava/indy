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

import static org.commonjava.aprox.depgraph.json.JsonUtils.deserializeRelationships;
import static org.commonjava.aprox.depgraph.json.JsonUtils.serializeRelationships;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class EProjectCycleSer
    implements JsonSerializer<EProjectCycle>, JsonDeserializer<EProjectCycle>
{

    @Override
    public JsonElement serialize( final EProjectCycle src, final Type typeOfSrc, final JsonSerializationContext context )
    {
        final JsonObject obj = new JsonObject();
        serializeRelationships( src.getAllRelationships(), obj, context );
        return obj;
    }

    @Override
    public EProjectCycle deserialize( final JsonElement json, final Type typeOfT, final JsonDeserializationContext context )
        throws JsonParseException
    {
        final JsonObject obj = json.getAsJsonObject();

        final Collection<ProjectRelationship<?>> rels = deserializeRelationships( obj, context );
        return new EProjectCycle( new ArrayList<ProjectRelationship<?>>( rels ) );
    }

}
