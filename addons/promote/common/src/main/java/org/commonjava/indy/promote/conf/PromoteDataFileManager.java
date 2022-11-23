/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.promote.conf;

import org.commonjava.indy.subsys.datafile.DataFile;
import org.commonjava.indy.subsys.datafile.DataFileManager;
import org.commonjava.indy.subsys.datafile.change.DataFileEventManager;
import org.commonjava.indy.subsys.datafile.conf.DataFileConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;

@ApplicationScoped
public class PromoteDataFileManager
{
    @Inject
    private PromoteConfig config;

    @Inject
    private DataFileEventManager fileEventManager;

    public PromoteDataFileManager()
    {
        this.config = new PromoteConfig();
    }

    public PromoteDataFileManager( final File rootDir, final DataFileEventManager fileEventManager )
    {
        this();
        this.fileEventManager = fileEventManager;
        this.config.setBasedir( rootDir );
    }

    public PromoteDataFileManager( final PromoteConfig config, final DataFileEventManager fileEventManager )
    {
        this.config = config;
        this.fileEventManager = fileEventManager;
    }

    public DataFile getDataFile( final String... pathParts )
    {
        final File base = config.getBasedir();
        final File f = Paths.get( base.getAbsolutePath(), pathParts )
                            .toFile();

        return new DataFile( f, fileEventManager );
    }

}
