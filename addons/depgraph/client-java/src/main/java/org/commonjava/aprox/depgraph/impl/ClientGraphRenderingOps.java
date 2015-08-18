/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.aprox.depgraph.impl;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.commonjava.aprox.client.core.AproxClientException;
import org.commonjava.aprox.depgraph.client.DepgraphAproxClientModule;
import org.commonjava.cartographer.CartoDataException;
import org.commonjava.cartographer.CartoRequestException;
import org.commonjava.cartographer.ops.GraphRenderingOps;
import org.commonjava.cartographer.request.MultiRenderRequest;
import org.commonjava.cartographer.request.PomRequest;
import org.commonjava.cartographer.request.RepositoryContentRequest;
import org.commonjava.maven.atlas.graph.traverse.print.StructureRelationshipPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

/**
 * Created by jdcasey on 8/17/15.
 */
public class ClientGraphRenderingOps
        implements GraphRenderingOps
{

    private ClientCartographer carto;

    private final DepgraphAproxClientModule module;

    public ClientGraphRenderingOps( ClientCartographer carto, DepgraphAproxClientModule module )
    {
        this.carto = carto;
        this.module = module;
    }

    @Override
    public void depTree( RepositoryContentRequest request, boolean collapseTransitives, PrintWriter writer )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            String output = module.depTree( carto.normalizeRequest( request ) );

            writer.write( output );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public void depTree( RepositoryContentRequest request, boolean collapseTransitives,
                         StructureRelationshipPrinter relPrinter, PrintWriter writer )
            throws CartoDataException, CartoRequestException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.warn(
                "StructureRelationshipPrinter and collapseTransitives parameters are ignored in this implementation! Using server-side defaults instead." );
        depList( request, writer );
    }

    @Override
    public void depList( RepositoryContentRequest request, PrintWriter writer )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            String output = module.depList( carto.normalizeRequest( request ) );

            writer.write( output );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }

    @Override
    public void depList( RepositoryContentRequest request, StructureRelationshipPrinter relPrinter, PrintWriter writer )
            throws CartoDataException, CartoRequestException
    {
        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.warn(
                "StructureRelationshipPrinter parameter is ignored in this implementation! Using server-side instance instead." );
        depList( request, writer );
    }

    @Override
    public Model generatePOM( PomRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            String xml = module.pom( carto.normalizeRequest( request ) );
            if ( xml == null )
            {
                return null;
            }

            return new MavenXpp3Reader().read( new StringReader( xml ), false );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
        catch ( XmlPullParserException | IOException e )
        {
            throw new CartoDataException( "Invalid output pom.xml: " + e.getMessage(), e );
        }
    }

    @Override
    public String dotfile( MultiRenderRequest request )
            throws CartoDataException, CartoRequestException
    {
        try
        {
            return module.dotfile( carto.normalizeRequest( request ) );
        }
        catch ( AproxClientException e )
        {
            throw new CartoDataException( "Failed to execute: " + e.getMessage(), e );
        }
    }
}
