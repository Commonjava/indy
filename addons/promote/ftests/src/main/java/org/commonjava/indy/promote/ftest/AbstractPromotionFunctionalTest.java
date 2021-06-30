/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.promote.ftest;

import org.apache.commons.io.FileUtils;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;

import java.io.File;
import java.io.IOException;

public class AbstractPromotionFunctionalTest
        extends AbstractIndyFunctionalTest
{
    protected File promoteDataDir;

    @Override
    protected void initTestData( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestData( fixture );
        promoteDataDir = new File( fixture.getBootOptions().getHomeDir(), "data/promote" );
        logger.info( "Promotion base data file dir: {}", promoteDataDir.getAbsolutePath() );
    }

    protected void writePromoteDataFile( String path, String contents )
            throws IOException
    {
        File file = new File( promoteDataDir.getAbsolutePath(), path );

        logger.info( "Writing promote data file to: {}\n\n{}\n\n", file.getAbsolutePath(), contents );
        file.getParentFile().mkdirs();

        FileUtils.write( file, contents );
    }
}
