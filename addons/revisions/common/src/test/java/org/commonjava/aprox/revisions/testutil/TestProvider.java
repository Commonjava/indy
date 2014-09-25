package org.commonjava.aprox.revisions.testutil;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;

import org.commonjava.aprox.conf.AproxConfiguration;
import org.commonjava.aprox.core.conf.DefaultAproxConfiguration;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.subsys.datafile.conf.DataFileConfiguration;
import org.commonjava.maven.galley.maven.internal.type.StandardTypeMapper;
import org.commonjava.maven.galley.maven.parse.XMLInfrastructure;
import org.commonjava.maven.galley.maven.spi.type.TypeMapper;
import org.commonjava.maven.galley.nfc.MemoryNotFoundCache;
import org.commonjava.maven.galley.spi.nfc.NotFoundCache;
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

    private AproxConfiguration aproxConfig;

    private DataFileConfiguration dataConfig;

    @PostConstruct
    public void init()
    {
        this.storageProviderConfig = new DefaultStorageProviderConfiguration( TEMP.newFolder( "storage" ) );
        this.dataConfig = new DataFileConfiguration( TEMP.newFolder( "data" ), TEMP.newFolder( "work" ) );

        this.nfc = new MemoryNotFoundCache();
        this.xmlInfra = new XMLInfrastructure();
        this.typeMapper = new StandardTypeMapper();
        this.aproxConfig = new DefaultAproxConfiguration();
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
    public AproxConfiguration getAproxConfig()
    {
        return aproxConfig;
    }

    @Produces
    @TestData
    @Default
    public DataFileConfiguration getDataConfig()
    {
        return dataConfig;
    }

}
