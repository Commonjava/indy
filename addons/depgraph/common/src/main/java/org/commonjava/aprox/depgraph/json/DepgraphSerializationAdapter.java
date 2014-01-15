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

import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectDirectRelationships;
import org.commonjava.maven.atlas.graph.model.EProjectGraph;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
import org.commonjava.maven.atlas.graph.model.EProjectWeb;
import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;
import org.commonjava.maven.atlas.ident.version.SingleVersion;
import org.commonjava.maven.cartographer.data.CartoDataManager;
import org.commonjava.web.json.ser.WebSerializationAdapter;

import com.google.gson.GsonBuilder;

public class DepgraphSerializationAdapter
    implements WebSerializationAdapter
{

    private final CartoDataManager data;

    public DepgraphSerializationAdapter( final CartoDataManager data )
    {
        this.data = data;
    }

    @Override
    public void register( final GsonBuilder gson )
    {
        gson.registerTypeAdapter( ArtifactRef.class, new ArtifactRefSer() );
        gson.registerTypeAdapter( EProjectGraph.class, new EProjectGraphSer( data ) );
        gson.registerTypeAdapter( EProjectCycle.class, new EProjectCycleSer() );
        gson.registerTypeAdapter( EProjectKey.class, new EProjectKeySer() );
        gson.registerTypeAdapter( EProjectDirectRelationships.class, new EProjectRelsSer() );
        gson.registerTypeAdapter( EProjectWeb.class, new EProjectWebSer( data ) );
        gson.registerTypeAdapter( ProjectRef.class, new ProjectRefSer() );
        gson.registerTypeAdapter( ProjectRelationship.class, new ProjectRelationshipSer() );
        gson.registerTypeAdapter( ParentRelationship.class, new ProjectRelationshipSer() );
        gson.registerTypeAdapter( DependencyRelationship.class, new ProjectRelationshipSer() );
        gson.registerTypeAdapter( PluginRelationship.class, new ProjectRelationshipSer() );
        gson.registerTypeAdapter( ExtensionRelationship.class, new ProjectRelationshipSer() );
        gson.registerTypeAdapter( PluginDependencyRelationship.class, new ProjectRelationshipSer() );
        gson.registerTypeAdapter( ProjectVersionRef.class, new ProjectVersionRefSer() );
        gson.registerTypeAdapter( SingleVersion.class, new SingleVersionSer() );
    }

}
