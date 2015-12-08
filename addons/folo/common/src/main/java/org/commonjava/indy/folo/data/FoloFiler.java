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
package org.commonjava.indy.folo.data;

import org.commonjava.indy.folo.model.TrackingKey;
import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;

import javax.inject.Inject;
import java.io.File;

/**
 * Created by jdcasey on 9/9/15.
 */
public class FoloFiler
{

    private static final String DATA_DIR = "folo";

    @Inject
    private DataFileManager dataFileManager;

    protected FoloFiler(){}

    public FoloFiler( DataFileManager dataFileManager )
    {
        this.dataFileManager = dataFileManager;
    }

    public DataFile getRecordFile( final TrackingKey key )
    {
        return getDataFile( key, FoloFileTypes.RECORD_JSON );
    }

    private DataFile getDataFile( TrackingKey key, String ext )
    {
        final String fname = String.format( "%s.%s", key.getId(), ext );

        return dataFileManager.getDataFile( DATA_DIR, fname );
    }

    public DataFile getRepositoryZipFile( final TrackingKey key )
    {
        return getDataFile( key, FoloFileTypes.REPO_ZIP );
    }

    public void deleteFiles( TrackingKey key )
    {
        for ( String ext : FoloFileTypes.TYPES )
        {
            File f = getDataFile( key, ext ).getDetachedFile();
            if ( f.exists() )
            {
                f.delete();
            }
        }
    }

}
