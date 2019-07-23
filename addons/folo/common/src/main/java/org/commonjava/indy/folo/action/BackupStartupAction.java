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
package org.commonjava.indy.folo.action;

import org.commonjava.indy.IndyWorkflowException;
import org.commonjava.indy.action.IndyLifecycleException;
import org.commonjava.indy.action.StartupAction;
import org.commonjava.indy.folo.ctl.FoloAdminController;
import org.commonjava.indy.folo.data.FoloFiler;

import javax.inject.Inject;
import java.io.File;

import static org.commonjava.indy.folo.ctl.FoloConstants.TRACKING_TYPE.SEALED;

/**
 * Created by ruhan on 8/2/18.
 */
public class BackupStartupAction implements StartupAction
{
    @Inject
    private FoloFiler filer;

    @Inject
    private FoloAdminController adminController;

    @Override
    public void start() throws IndyLifecycleException
    {
        File dir = filer.getBackupDir( SEALED.getValue() ).getDetachedFile(); // data/folo/bak/sealed

        // if dir not exist, do initial back up for sealed
        if ( !dir.isDirectory() )
        {
            dir.mkdirs();
            try
            {
                adminController.doInitialBackUpForSealed();
            }
            catch ( IndyWorkflowException e )
            {
                throw new IndyLifecycleException( "doInitialBackUpForSealed fail", e );
            }
        }
    }

    @Override
    public String getId()
    {
        return "folo-backup-sealed-records";
    }

    @Override
    public int getStartupPriority()
    {
        return 90;
    }
}
