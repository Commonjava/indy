/**
 * Copyright (C) 2013~2019 Red Hat, Inc.
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
package org.commonjava.indy.pathmap.migrate;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Util
{
    static final String DEFAULT_BASE_DIR = "/opt/indy/var/lib/indy/storage";

    static final String DEFAULT_WORK_DIR = "./";

    static final int DEFAULT_THREADS_NUM = 1;

    static final int DEFAULT_BATCH_SIZE = 100000;

    static final String TODO_FILES_DIR = "todo";

    static final String PROCESSED_FILES_DIR = "processed";

    static final String FAILED_PATHS_FILE = "failed_paths";

    static final String STATUS_FILE = "scan_status";

    static final String CMD_SCAN = "scan";

    static final String CMD_MIGRATE = "migrate";

    static void prepareWorkingDir( final String workDir )
            throws IOException
    {
        Path todoDir = Paths.get( workDir, TODO_FILES_DIR );
        if ( todoDir.toFile().exists() )
        {
            System.out.println( "todo folder is not empty, will clean it first." );
            FileUtils.forceDelete( todoDir.toFile() );
        }
        Files.createDirectories( todoDir );
        Path processedDir = Paths.get( workDir, PROCESSED_FILES_DIR );
        if ( processedDir.toFile().exists() )
        {
            System.out.println( "processed folder is not empty, will clean it first." );
            FileUtils.forceDelete( processedDir.toFile() );
        }
        Files.createDirectories( processedDir );
    }

}
