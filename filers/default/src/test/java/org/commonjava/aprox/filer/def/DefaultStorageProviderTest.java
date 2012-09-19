package org.commonjava.aprox.filer.def;

import java.io.File;

import org.commonjava.aprox.core.data.FilerTCK;
import org.commonjava.aprox.filer.def.conf.DefaultStorageProviderConfiguration;
import org.commonjava.aprox.io.StorageProvider;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public class DefaultStorageProviderTest
    extends FilerTCK
{

    private File rootDir;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    public void setup()
    {
        rootDir = folder.newFolder( "test-filer-storage" );
    }

    @Override
    protected StorageProvider getStorageProvider()
        throws Exception
    {
        return new DefaultStorageProvider( new DefaultStorageProviderConfiguration( rootDir ) );
    }

}
