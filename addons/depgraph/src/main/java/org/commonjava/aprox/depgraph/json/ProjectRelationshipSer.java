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
import java.net.URI;
import java.net.URISyntaxException;

import org.commonjava.maven.atlas.graph.rel.DependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.ExtensionRelationship;
import org.commonjava.maven.atlas.graph.rel.ParentRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginDependencyRelationship;
import org.commonjava.maven.atlas.graph.rel.PluginRelationship;
import org.commonjava.maven.atlas.graph.rel.ProjectRelationship;
import org.commonjava.maven.atlas.graph.rel.RelationshipType;
import org.commonjava.maven.atlas.graph.util.RelationshipUtils;
import org.commonjava.maven.atlas.ident.DependencyScope;
import org.commonjava.maven.atlas.ident.ref.ArtifactRef;
import org.commonjava.maven.atlas.ident.ref.ProjectRef;
import org.commonjava.maven.atlas.ident.ref.ProjectVersionRef;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ProjectRelationshipSer
    implements JsonSerializer<ProjectRelationship<?>>, JsonDeserializer<ProjectRelationship<?>>
{

    @Override
    public ProjectRelationship<?> deserialize( final JsonElement src, final Type typeInfo,
                                               final JsonDeserializationContext ctx )
        throws JsonParseException
    {
        final JsonObject obj = src.getAsJsonObject();

        // TODO: Make allowances for migration from older serialized objects!
        //        final int jsonVersion = obj.get( SerializationConstants.JSON_VERSION )
        //                                   .getAsInt();
        //
        //        if ( jsonVersion != SerializationConstants.CURRENT_JSON_VERSION )
        //        {
        //        }

        final String type = obj.get( SerializationConstants.RELATIONSHIP_TYPE )
                               .getAsString();
        final String decl = obj.get( SerializationConstants.DECLARING_REF )
                               .getAsString();
        final JsonElement u = obj.get( SerializationConstants.POM_LOCATION_URI );
        URI pomLocation;
        try
        {
            pomLocation = u == null ? RelationshipUtils.POM_ROOT_URI : new URI( u.getAsString() );
        }
        catch ( final URISyntaxException e )
        {
            throw new JsonParseException( "Invalid pom-location-uri: '" + u.getAsString() + "': " + e.getMessage(), e );
        }

        final RelationshipType rt = RelationshipType.getType( type );

        final ProjectVersionRef declaring = JsonUtils.parseProjectVersionRef( decl );

        switch ( rt )
        {
            case DEPENDENCY:
            {
                final String tgt = getRawTarget( obj );
                final ArtifactRef target = JsonUtils.parseArtifactRef( tgt );

                final JsonElement scp = obj.get( SerializationConstants.SCOPE );
                final DependencyScope scope =
                    scp == null ? DependencyScope.compile : DependencyScope.getScope( scp.getAsString() );

                return new DependencyRelationship( pomLocation, declaring, target, scope, getIndex( obj ),
                                                   isManaged( obj ) );
            }
            case EXTENSION:
            {
                final String tgt = getRawTarget( obj );
                final ProjectVersionRef target = JsonUtils.parseProjectVersionRef( tgt );

                return new ExtensionRelationship( pomLocation, declaring, target, getIndex( obj ) );
            }
            case PARENT:
            {
                final String tgt = getRawTarget( obj );
                final ProjectVersionRef target = JsonUtils.parseProjectVersionRef( tgt );

                return new ParentRelationship( pomLocation, declaring, target );
            }
            case PLUGIN:
            {
                final String tgt = getRawTarget( obj );
                final ProjectVersionRef target = JsonUtils.parseProjectVersionRef( tgt );

                return new PluginRelationship( pomLocation, declaring, target, getIndex( obj ), isManaged( obj ) );
            }
            case PLUGIN_DEP:
            {
                final String plug = obj.get( SerializationConstants.PLUGIN_REF )
                                       .getAsString();
                final ProjectRef plugin = JsonUtils.parseProjectRef( plug );

                final String tgt = getRawTarget( obj );
                final ArtifactRef target = JsonUtils.parseArtifactRef( tgt );

                return new PluginDependencyRelationship( pomLocation, declaring, plugin, target, getIndex( obj ),
                                                         isManaged( obj ) );
            }
            default:
            {
                throw new JsonParseException( "Unknown ProjectRelationship type: '" + rt + "'." );
            }
        }
    }

    private boolean isManaged( final JsonObject obj )
    {
        final JsonElement mgd = obj.get( SerializationConstants.MANAGED );
        return mgd == null ? false : mgd.getAsBoolean();
    }

    private int getIndex( final JsonObject obj )
    {
        final JsonElement idx = obj.get( SerializationConstants.INDEX );
        return idx == null ? -1 : idx.getAsInt();
    }

    private String getRawTarget( final JsonObject obj )
    {
        return obj.get( SerializationConstants.TARGET_REF )
                  .getAsString();
    }

    @Override
    public JsonElement serialize( final ProjectRelationship<?> src, final Type type, final JsonSerializationContext ctx )
    {
        final JsonObject obj = new JsonObject();
        obj.addProperty( SerializationConstants.JSON_VERSION, SerializationConstants.CURRENT_JSON_VERSION );

        obj.addProperty( SerializationConstants.RELATIONSHIP_TYPE, src.getType()
                                                                      .name() );
        obj.addProperty( SerializationConstants.DECLARING_REF, JsonUtils.formatRef( src.getDeclaring() ) );
        obj.addProperty( SerializationConstants.TARGET_REF, JsonUtils.formatRef( src.getTarget() ) );
        obj.addProperty( SerializationConstants.INDEX, src.getIndex() );

        switch ( src.getType() )
        {
            case DEPENDENCY:
            {
                obj.addProperty( SerializationConstants.SCOPE, ( (DependencyRelationship) src ).getScope()
                                                                                               .realName() );
                break;
            }
            case PLUGIN_DEP:
            {
                obj.addProperty( SerializationConstants.PLUGIN_REF,
                                 JsonUtils.formatRef( ( (PluginDependencyRelationship) src ).getPlugin() ) );
                break;
            }
            default:
            {
            }
        }

        return obj;
    }

}
