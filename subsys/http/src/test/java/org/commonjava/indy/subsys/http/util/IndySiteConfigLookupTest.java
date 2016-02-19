package org.commonjava.indy.subsys.http.util;

import org.commonjava.indy.audit.ChangeSummary;
import org.commonjava.indy.data.IndyDataException;
import org.commonjava.indy.mem.data.MemoryStoreDataManager;
import org.commonjava.indy.model.core.RemoteRepository;
import org.commonjava.util.jhttpc.model.SiteConfig;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by jdcasey on 2/15/16.
 */
public class IndySiteConfigLookupTest
{
    @Test
    public void checkServerCertPemIsConfigured()
            throws IndyDataException
    {
        RemoteRepository remote = new RemoteRepository( "test", "http://test.com/repo" );
        remote.setServerCertPem( "AAAAFFFFFSDADFADSFASDFASDFASDFASDFASDFsa" );
        remote.setServerTrustPolicy( "self-signed" );

        MemoryStoreDataManager storeData = new MemoryStoreDataManager(true);
        storeData.storeArtifactStore( remote, new ChangeSummary( ChangeSummary.SYSTEM_USER, "This is a test" ) );

        IndySiteConfigLookup lookup = new IndySiteConfigLookup( storeData );
        SiteConfig siteConfig = lookup.lookup( "remote:test" );

        assertThat( siteConfig.getServerCertPem(), equalTo( remote.getServerCertPem() ) );
    }
}
