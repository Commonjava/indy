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
package org.commonjava.indy.promote.change;

import org.commonjava.indy.promote.conf.PromoteConfig;
import org.commonjava.indy.promote.data.PromotionException;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.template.ScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class PromoteChangeManager
{

    private final Logger logger = LoggerFactory.getLogger( getClass() );

    private final static String FORMATTER_SCRIPT = "change/tracking-id-formatter.groovy";

    @Inject
    private DataFileManager ffManager;

    @Inject
    private PromoteConfig config;

    @Inject
    private ScriptEngine scriptEngine;

    private TrackingIdFormatter formatter;

    public TrackingIdFormatter getTrackingIdFormatter()
    {
        return formatter;
    }

    protected PromoteChangeManager()
    {
    }

    public PromoteChangeManager( final DataFileManager ffManager, final PromoteConfig config )
                    throws PromotionException
    {
        this.ffManager = ffManager;
        this.config = config;
        this.formatter = parseTrackingIdFormatter();
    }

    @PostConstruct
    public void cdiInit()
    {
        try
        {
            formatter = parseTrackingIdFormatter();
        }
        catch ( final Exception e )
        {
            logger.error( "Failed to parse tracking id formatter: " + e.getMessage(), e );
        }
    }

    public TrackingIdFormatter parseTrackingIdFormatter() throws PromotionException
    {
        DataFile dataFile = ffManager.getDataFile( config.getBasedir(), FORMATTER_SCRIPT );
        if ( dataFile.exists() )
        {
            try
            {
                String spec = dataFile.readString();
                TrackingIdFormatter formatter = scriptEngine.parseScriptInstance( spec, TrackingIdFormatter.class );
                logger.debug( "Parsed: {}", formatter.getClass().getName() );
                return formatter;
            }
            catch ( final Exception e )
            {
                throw new PromotionException( "[PROMOTE] Cannot load from: {} as an instance of: {}. Reason: {}",
                                              FORMATTER_SCRIPT, TrackingIdFormatter.class.getSimpleName(),
                                              e.getMessage() );
            }
        }
        logger.debug( "No tracking-id-formatter.groovy was defined for promotion, use default formatter." );
        return storeKey -> storeKey.getName();
    }

}
