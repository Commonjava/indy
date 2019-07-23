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
package org.commonjava.indy.hostedbyarc.ftests;

import org.apache.commons.io.IOUtils;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.ftest.core.AbstractIndyFunctionalTest;
import org.commonjava.indy.hostedbyarc.client.IndyHostedByArchiveClientModule;
import org.commonjava.indy.test.fixture.core.CoreServerFixture;
import org.junit.Before;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.fail;

public abstract class AbstractHostedByArcTest
        extends AbstractIndyFunctionalTest
{
    private IndyHostedByArchiveClientModule hostedByArchiveClientModule = new IndyHostedByArchiveClientModule();

    private File zipFile;

    @Before
    public void prepare()
            throws Exception
    {
        final String ZIP_FILE = getZipFileResource();
        zipFile = getTemp().newFile( ZIP_FILE );

        try (InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream( ZIP_FILE );
             OutputStream zipFileOut = new FileOutputStream( zipFile ))
        {
            if ( stream == null )
            {
                fail( "Cannot find classpath test zip: " + ZIP_FILE );
            }

            IOUtils.copy( stream, zipFileOut );
        }

        if ( zipFile == null || !zipFile.exists() )
        {
            throw new IllegalStateException( String.format( "Zip file %s not found", ZIP_FILE ) );
        }
    }

    @Override
    protected Collection<IndyClientModule> getAdditionalClientModules()
    {
        return Collections.singletonList( hostedByArchiveClientModule );
    }

    @Override
    protected void initTestConfig( CoreServerFixture fixture )
            throws IOException
    {
        super.initTestConfig( fixture );
        writeConfigFile( "conf.d/threadpools.conf", "[threadpools]\nenabled=true" );
        if ( enabled() )
        {
            writeConfigFile( "conf.d/hosted-by-archive.conf", "[hosted-by-archive]\nenabled=true" );
        }
    }



    protected File getZipFile()
    {
        return zipFile;
    }

    protected abstract String getZipFileResource();

    protected abstract boolean enabled();


}
