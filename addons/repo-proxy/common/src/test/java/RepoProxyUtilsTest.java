import org.commonjava.indy.repo.proxy.RepoProxyUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

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
}
