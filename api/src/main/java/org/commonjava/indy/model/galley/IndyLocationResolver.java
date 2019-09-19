/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.model.galley;

import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.data.StoreDataManager;
import org.commonjava.indy.model.core.ArtifactStore;
import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.util.LocationUtils;
import org.commonjava.maven.galley.TransferException;
import org.commonjava.maven.galley.model.Location;
import org.commonjava.maven.galley.spi.transport.LocationResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class IndyLocationResolver
    implements LocationResolver
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    @Inject
    private StoreDataManager dataManager;

    protected IndyLocationResolver()
    {
    }

    public IndyLocationResolver( final StoreDataManager dataManager )
    {
        this.dataManager = dataManager;
    }

    @Override
    public Location resolve( final String spec )
        throws TransferException
    {
        ArtifactStore store;
        try
        {
            final StoreKey source = StoreKey.fromString( spec );
            if ( source == null )
            {
                throw new TransferException(
                                             "Failed to parse StoreKey (format: '[remote|hosted|group]:name') from: '%s'." );
            }

            store = dataManager.getArtifactStore( source );
        }
        catch ( final IndyDataException e )
        {
            throw new TransferException( "Cannot find ArtifactStore to match source key: %s. Reason: %s", e, spec,
                                         e.getMessage() );
        }

        if ( store == null )
        {
            throw new TransferException( "Cannot find ArtifactStore to match source key: %s.", spec );
        }

        final KeyedLocation location = LocationUtils.toLocation( store );
        logger.debug( "resolved source location: '{}' to: '{}'", spec, location );

        return location;
    }

}
