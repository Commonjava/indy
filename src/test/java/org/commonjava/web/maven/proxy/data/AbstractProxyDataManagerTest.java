package org.commonjava.web.maven.proxy.data;

import static org.commonjava.couch.test.fixture.LoggingFixture.setupLogging;

import org.apache.log4j.Level;
import org.commonjava.auth.couch.conf.DefaultUserManagerConfig;
import org.commonjava.auth.couch.data.PasswordManager;
import org.commonjava.auth.couch.data.UserDataManager;
import org.commonjava.couch.conf.CouchDBConfiguration;
import org.commonjava.couch.conf.DefaultCouchDBConfiguration;
import org.commonjava.couch.db.CouchManager;
import org.commonjava.web.maven.proxy.conf.DefaultProxyConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

public class AbstractProxyDataManagerTest
{
    protected static ProxyDataManager manager;

    protected static DefaultProxyConfiguration config;

    protected static DefaultUserManagerConfig umConfig;

    protected static CouchManager couch;

    protected static PasswordManager passwordManager;

    protected static UserDataManager userManager;

    @BeforeClass
    public static void setupManager()
    {
        setupLogging( Level.DEBUG );

        config = new DefaultProxyConfiguration();

        umConfig = new DefaultUserManagerConfig( "admin@nowhere.com", "password", "Admin", "User" );

        CouchDBConfiguration couchConfig =
            new DefaultCouchDBConfiguration( "http://developer.commonjava.org/db/test-aprox" );

        couch = new CouchManager( couchConfig );

        passwordManager = new PasswordManager();
        userManager = new UserDataManager( umConfig, passwordManager, couch );

        manager = new ProxyDataManager( config, userManager, couchConfig, couch );
    }

    @Before
    public void setupDB()
        throws Exception
    {
        if ( couch.dbExists() )
        {
            couch.dropDatabase();
        }
        manager.install();
    }

    @After
    public void teardownDB()
        throws Exception
    {
        couch.dropDatabase();
    }

}
