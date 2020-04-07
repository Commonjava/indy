import org.commonjava.indy.model.core.StoreKey;
import org.commonjava.indy.repo.proxy.RepoProxyUtils;
import org.junit.Test;

import java.util.Optional;

import static org.commonjava.indy.model.core.StoreKey.fromString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class RepoProxyUtilsTest
{
    @Test
    public void testGetOriginalStoreKeyFromPath()
    {
        String path = "/api/content/maven/group/abc/org/commonjava/indy/maven-metadata.xml";
        Optional<String> storeKeyStr = RepoProxyUtils.getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "maven:group:abc" ) );

        path = "/api/content/maven/hosted/def";
        storeKeyStr = RepoProxyUtils.getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "maven:hosted:def" ) );

        path = "/api/content/npm/group/ghi/";
        storeKeyStr = RepoProxyUtils.getOriginalStoreKeyFromPath( path );
        assertTrue( storeKeyStr.isPresent() );
        assertThat( storeKeyStr.get(), equalTo( "npm:group:ghi" ) );

        path = "/api/content/npm/groupe/ghi/";
        storeKeyStr = RepoProxyUtils.getOriginalStoreKeyFromPath( path );
        assertFalse( storeKeyStr.isPresent() );
    }

    @Test
    public void testExtractPath()
    {
        String fullPath = "/api/content/maven/group/abc/org/commonjava/indy/maven-metadata.xml";
        String repoPath = "maven/group/abc";
        String path = RepoProxyUtils.extractPath( fullPath, repoPath );
        assertThat( path, equalTo( "/org/commonjava/indy/maven-metadata.xml" ) );

        fullPath = "/api/content/maven/hosted/def/org/commonjava/indy/1.0/";
        repoPath = "/maven/hosted/def";
        path = RepoProxyUtils.extractPath( fullPath, repoPath );
        assertThat( path, equalTo( "/org/commonjava/indy/1.0/" ) );

        fullPath = "/api/content/npm/group/ghi/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom";
        repoPath = "npm/group/ghi/";
        path = RepoProxyUtils.extractPath( fullPath, repoPath );
        assertThat( path, equalTo( "/org/commonjava/indy/indy-api/1.0/indy-api-1.0.pom" ) );

        fullPath = "/api/content/npm/hosted/jkl/org/commonjava/indy/indy-api/2.0/indy-api-2.0.jar";
        repoPath = "/npm/hosted/jkl/";
        path = RepoProxyUtils.extractPath( fullPath, repoPath );
        assertThat( path, equalTo( "/org/commonjava/indy/indy-api/2.0/indy-api-2.0.jar" ) );
    }

    @Test
    public void testProxyTo()
    {
        String fullPath = "/api/content/maven/group/abc/org/commonjava/indy/maven-metadata.xml";
        Optional<String> proxyTo = RepoProxyUtils.getProxyTo( fullPath, StoreKey.fromString( "maven:remote:group-abc" ) );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/content/maven/remote/group-abc/org/commonjava/indy/maven-metadata.xml" ) );

        fullPath = "/api/browse/content/maven/hosted/def/org/commonjava/indy/1.0/";
        proxyTo = RepoProxyUtils.getProxyTo( fullPath, StoreKey.fromString( "maven:remote:hosted-def" ) );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/browse/content/maven/remote/hosted-def/org/commonjava/indy/1.0/" ) );

        fullPath = "/api/folo/track/npm/group/ghi/jquery";
        proxyTo = RepoProxyUtils.getProxyTo( fullPath, StoreKey.fromString( "npm:remote:group-ghi" ) );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/folo/track/npm/remote/group-ghi/jquery" ) );

        fullPath = "/api/folo/track/npm/hosted/jkl/@react/core";
        proxyTo = RepoProxyUtils.getProxyTo( fullPath, StoreKey.fromString( "npm:remote:hosted-jkl") );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( "/api/folo/track/npm/remote/hosted-jkl/@react/core" ) );
    }

    @Test
    public void testProxyToStoreKey()
    {
        String fullPath = "/api/content/maven/group/abc/org/commonjava/indy/maven-metadata.xml";
        Optional<StoreKey> proxyTo = RepoProxyUtils.getProxyToStoreKey( fullPath );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( fromString( "maven:remote:abc" ) ) );

        fullPath = "/api/browse/content/maven/hosted/def/org/commonjava/indy/1.0/";
        proxyTo = RepoProxyUtils.getProxyToStoreKey( fullPath );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( fromString( "maven:remote:def" ) ) );

        fullPath = "/api/folo/track/npm/group/ghi/jquery";
        proxyTo = RepoProxyUtils.getProxyToStoreKey( fullPath );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( fromString( "npm:remote:ghi" ) ) );

        fullPath = "/api/folo/track/npm/hosted/jkl/@react/core";
        proxyTo = RepoProxyUtils.getProxyToStoreKey( fullPath );
        assertTrue( proxyTo.isPresent() );
        assertThat( proxyTo.get(), equalTo( fromString( "npm:remote:jkl" ) ) );
    }
}
