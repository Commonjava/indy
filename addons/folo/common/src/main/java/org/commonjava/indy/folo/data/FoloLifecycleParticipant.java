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
package org.commonjava.indy.folo.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;

@Named
public class FoloLifecycleParticipant
    implements StartupAction
{

    private static final String FOLO_ID = "folo";

    private static final String FOLO_DIRECTORY_IGNORE = "folo";

    @Inject
    private DataFileManager dataFileManager;

    protected FoloLifecycleParticipant()
    {
    }

    public FoloLifecycleParticipant( final DataFileManager dataFileManager )
    {
        this.dataFileManager = dataFileManager;
    }

    @Override
    public String getId()
    {
        return FOLO_ID;
    }

    @Override
    public int getStartupPriority()
    {
        return 40;
    }

    @Override
    public void start()
        throws IndyLifecycleException
    {
        try
        {
            final DataFile dataFile = dataFileManager.getDataFile( ".gitignore" );
            final List<String> lines = dataFile.exists() ? dataFile.readLines() : new ArrayList<String>();
            if ( !lines.contains( FOLO_DIRECTORY_IGNORE ) )
            {
                lines.add( FOLO_DIRECTORY_IGNORE );

                dataFile.writeLines( lines, new ChangeSummary( ChangeSummary.SYSTEM_USER,
                                                               "Adding artimon to ignored list." ) );
            }
        }
        catch ( final IOException e )
        {
            throw new IndyLifecycleException(
                                               "Failed while attempting to access .gitignore for data directory (trying to add artimon dir to ignores list).",
                                               e );
        }
    }

}
