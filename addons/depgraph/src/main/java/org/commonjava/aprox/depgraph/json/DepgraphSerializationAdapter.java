package org.commonjava.aprox.depgraph.json;

import org.commonjava.maven.atlas.graph.EGraphManager;
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
import org.commonjava.maven.cartographer.data.GraphWorkspaceHolder;
import org.commonjava.web.json.ser.WebSerializationAdapter;

import com.google.gson.GsonBuilder;

public class DepgraphSerializationAdapter
    implements WebSerializationAdapter
{

    private final EGraphManager graphs;

    private final GraphWorkspaceHolder sessionManager;

    public DepgraphSerializationAdapter( final EGraphManager graphs, final GraphWorkspaceHolder sessionManager )
    {
        this.graphs = graphs;
        this.sessionManager = sessionManager;
    }

    @Override
    public void register( final GsonBuilder gson )
    {
        gson.registerTypeAdapter( ArtifactRef.class, new ArtifactRefSer() );
        gson.registerTypeAdapter( EProjectGraph.class, new EProjectGraphSer( graphs, sessionManager ) );
        gson.registerTypeAdapter( EProjectCycle.class, new EProjectCycleSer() );
        gson.registerTypeAdapter( EProjectKey.class, new EProjectKeySer() );
        gson.registerTypeAdapter( EProjectDirectRelationships.class, new EProjectRelsSer() );
        gson.registerTypeAdapter( EProjectWeb.class, new EProjectWebSer( graphs, sessionManager ) );
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
