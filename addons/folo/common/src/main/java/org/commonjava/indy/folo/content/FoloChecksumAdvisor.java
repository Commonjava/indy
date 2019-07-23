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
package org.commonjava.indy.folo.content;

import org.commonjava.indy.content.IndyChecksumAdvisor;
import org.commonjava.indy.folo.ctl.FoloConstants;
import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.maven.galley.event.EventMetadata;
import org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor;
import org.commonjava.maven.galley.model.Transfer;
import org.commonjava.maven.galley.model.TransferOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.Optional;

import static org.commonjava.maven.galley.io.checksum.ChecksummingDecoratorAdvisor.ChecksumAdvice.CALCULATE_NO_WRITE;

/**
 * Created by jdcasey on 5/5/17.
 */
@ApplicationScoped
public class FoloChecksumAdvisor
        implements IndyChecksumAdvisor
{
    @Override
    public Optional<ChecksummingDecoratorAdvisor.ChecksumAdvice> getChecksumReadAdvice( final Transfer transfer,
                                                                                        final TransferOperation operation,
                                                                                        final EventMetadata eventMetadata )
    {
        return Optional.empty();
    }

    @Override
    public Optional<ChecksummingDecoratorAdvisor.ChecksumAdvice> getChecksumWriteAdvice( final Transfer transfer,
                                                                                         final TransferOperation operation,
                                                                                         final EventMetadata eventMetadata )
    {
        final TrackingKey trackingKey = (TrackingKey) eventMetadata.get( FoloConstants.TRACKING_KEY );
        if ( trackingKey == null )
        {
            return Optional.empty();
        }

        Logger logger = LoggerFactory.getLogger( getClass() );
        logger.debug( "Enabling checksumming for {} of: {} with tracking key: {}", operation, transfer, trackingKey );
        return Optional.of( CALCULATE_NO_WRITE );
    }
}
