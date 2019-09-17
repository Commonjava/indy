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
package org.commonjava.indy.revisions.testutil;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.indy.conf.IndyConfiguration;
import org.commonjava.indy.conf.DefaultIndyConfiguration;
import org.commonjava.indy.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.indy.inject.TestData;
import org.commonjava.indy.model.core.io.IndyObjectMapper;
import org.commonjava.indy.subsys.datafile.conf.DataFileConfiguration;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
import org.junit.Assert;
import org.junit.rules.TemporaryFolder;

@ApplicationScoped
public class TestProvider
{
    
    private static TemporaryFolder TEMP;

    public static void setTemporaryFolder( final TemporaryFolder temp )
    {
        TEMP = temp;
    }

    private DefaultStorageProviderConfiguration storageProviderConfig;

    private NotFoundCache nfc;

    private XMLInfrastructure xmlInfra;

    private TypeMapper typeMapper;

    private IndyConfiguration indyConfig;

    private DataFileConfiguration dataConfig;

    private IndyObjectMapper objectMapper;

    @PostConstruct
    public void init()
    {
        try
        {
            this.storageProviderConfig = new DefaultStorageProviderConfiguration( TEMP.newFolder( "storage" ) );
            this.dataConfig = new DataFileConfiguration( TEMP.newFolder( "data" ), TEMP.newFolder( "work" ) );
        }
        catch ( final IOException e )
        {
            e.printStackTrace();
            Assert.fail( "Failed to setup temporary directory structures: " + e.getMessage() );
        }

        this.nfc = new MemoryNotFoundCache();
        this.xmlInfra = new XMLInfrastructure();
        this.typeMapper = new StandardTypeMapper();
        this.indyConfig = new DefaultIndyConfiguration();
        this.objectMapper = new IndyObjectMapper( true );
    }

    @Produces
    @Default
    @TestData
    public IndyObjectMapper getObjectMapper()
    {
        return objectMapper;
    }

    @Produces
    @TestData
    @Default
    public NotFoundCache getNfc()
    {
        return nfc;
    }

    @Produces
    @TestData
    @Default
    public DefaultStorageProviderConfiguration getStorageProviderConfig()
    {
        return storageProviderConfig;
    }

    @Produces
    @TestData
    @Default
    public XMLInfrastructure getXmlInfra()
    {
        return xmlInfra;
    }

    @Produces
    @TestData
    @Default
    public TypeMapper getTypeMapper()
    {
        return typeMapper;
    }

    @Produces
    @TestData
    @Default
    public IndyConfiguration getIndyConfig()
    {
        return indyConfig;
    }

    @Produces
    @TestData
    @Default
    public DataFileConfiguration getDataConfig()
    {
        return dataConfig;
    }

}
