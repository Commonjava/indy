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

import org.commonjava.aprox.depgraph.dto.GraphTransferDTO;
import org.commonjava.maven.atlas.graph.model.EProjectCycle;
import org.commonjava.maven.atlas.graph.model.EProjectKey;
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
import org.commonjava.web.json.ser.WebSerializationAdapter;

import com.google.gson.GsonBuilder;

public class DepgraphSerializationAdapter
    implements WebSerializationAdapter
{

    @Override
    public void register( final GsonBuilder gson )
    {
        gson.registerTypeAdapter( ArtifactRef.class, new ArtifactRefSer() );
        gson.registerTypeAdapter( EProjectCycle.class, new EProjectCycleSer() );
        gson.registerTypeAdapter( EProjectKey.class, new EProjectKeySer() );
        gson.registerTypeAdapter( GraphTransferDTO.class, new GraphTransferDTOSer() );
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
