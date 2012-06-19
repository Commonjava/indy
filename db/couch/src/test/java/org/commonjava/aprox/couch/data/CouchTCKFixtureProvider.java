package org.commonjava.aprox.couch.data;

import java.io.File;

import org.commonjava.aprox.core.conf.AproxConfiguration;
import org.commonjava.aprox.core.data.StoreDataManager;
import org.commonjava.aprox.core.data.TCKFixtureProvider;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.conf.DefaultCouchDBConfiguration;
import org.commonjava.couch.db.CouchDBException;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.couch.io.CouchAppReader;
import org.commonjava.couch.io.CouchHttpClient;
import org.commonjava.couch.io.Serializer;
import org.junit.rules.TemporaryFolder;

public class CouchTCKFixtureProvider
    extends TemporaryFolder
    implements TCKFixtureProvider
{
    private static final String DB_URL = "http://localhost:5984/test-aprox-db";

    private CouchStoreDataManager dataManager;

    private CouchManager couch;

    @Override
    public StoreDataManager getDataManager()
    {
        return dataManager;
    }

    @Override
    protected void before()
        throws Throwable
    {
        super.before();

        final Serializer serializer = new Serializer();

        final File repoDir = newFolder( "repo" );
        final AproxConfiguration config = new AproxConfiguration()
        {
            @Override
            public File getStorageRootDirectory()
            {
                return repoDir;
            }

            @Override
            public int getPassthroughTimeoutSeconds()
            {
                return 500;
            }
        };

        final CouchDBConfiguration couchConfig = new DefaultCouchDBConfiguration( DB_URL );
        final CouchHttpClient couchClient = new CouchHttpClient( couchConfig, serializer );

        couch = new CouchManager( couchConfig, couchClient, serializer, new CouchAppReader() );
        dataManager = new CouchStoreDataManager( config, couchConfig, couch, serializer );

        dataManager.install();
    }

    @Override
    protected void after()
    {
        try
        {
            couch.dropDatabase();
        }
        catch ( final CouchDBException e )
        {
            throw new RuntimeException( "Failed to drop CouchDB database: " + DB_URL + "\nReason: " + e.getMessage(), e );
        }

        super.after();
    }

}
