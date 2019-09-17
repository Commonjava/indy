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
    public static final String BAK_DIR = "bak";

    public static final String FOLO_DIR = "folo";

    public static final String FOLO_SEALED_ZIP = "folo-sealed.zip";

    public static final String FOLO_SEALED_DAT = "folo-sealed.dat";

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
        return dataFileManager.getDataFile( FOLO_DIR, fname );
    }

    public DataFile getRepositoryZipFile( final TrackingKey key )
    {
        return getDataFile( key, FoloFileTypes.REPO_ZIP );
    }

    public DataFile getSealedZipFile()
    {
        return dataFileManager.getDataFile( FOLO_DIR, FOLO_SEALED_ZIP );
    }

    public DataFile getSealedDataFile()
    {
        return dataFileManager.getDataFile( FOLO_DIR, FOLO_SEALED_DAT );
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

    public DataFile getBackupDir( String type )
    {
        return dataFileManager.getDataFile( FOLO_DIR, BAK_DIR, type ); // data/folo/bak/sealed
    }
}
