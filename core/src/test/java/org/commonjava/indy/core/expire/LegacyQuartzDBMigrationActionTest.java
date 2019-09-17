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
package org.commonjava.indy.core.expire;

import org.commonjava.indy.subsys.datafile.conf.DataFileConfiguration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by gli on 16-12-9.
 */
@Ignore("Obsolete")
public class LegacyQuartzDBMigrationActionTest
{
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private static final String INDY_HOME = "var/lib/indy";

    private DataFileConfiguration config;

    private LegacyQuartzDBMigrationAction action;

    private File data;

    @Before
    public void prepare()
            throws Exception
    {
        File file = temp.getRoot();
        data = new File( file, INDY_HOME + "/" + DataFileConfiguration.DEFAULT_DATA_SUBDIR );
        System.setProperty( "indy.home", file.getAbsolutePath() + "/" + INDY_HOME );
        data.mkdirs();
        new File( data, "scheduler.mv.db" ).createNewFile();
        new File( data, "scheduler.trace.db" ).createNewFile();
        new File( data, "content-index" ).mkdir();
        new File( data, "indy" ).mkdir();
        new File( data, "promote" ).mkdir();
        new File( data, "schedule.mv.db" ).createNewFile();
        config = new DataFileConfiguration();

        action = new LegacyQuartzDBMigrationAction( config );

    }

    @Test
    public void test()
            throws Exception
    {
        File dbMv = new File( data, "scheduler.mv.db" );
        File dbTrace = new File( data, "scheduler.mv.db" );
        File index = new File( data, "content-index" );
        File indy = new File( data, "indy" );
        File promote = new File( data, "promote" );
        File normal = new File( data, "schedule.mv.db" );
        assertTrue( dbMv.exists() );
        assertTrue( dbTrace.exists() );
        assertTrue( index.exists() );
        assertTrue( indy.exists() );
        assertTrue( promote.exists() );

        action.migrate();

        assertFalse( dbMv.exists() );
        assertFalse( dbTrace.exists() );
        assertTrue( index.exists() );
        assertTrue( indy.exists() );
        assertTrue( promote.exists() );
        assertTrue( normal.exists() );
    }
}
