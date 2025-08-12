package org.commonjava.indy.koji.util;

import org.commonjava.indy.core.content.PathMaskChecker;
import org.commonjava.indy.model.core.HostedRepository;
import org.commonjava.indy.model.core.RemoteRepository;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class PathMaskCheckerTest
{

    @Test
    public void extractGroupIdPath() throws Exception
    {
        List<Map.Entry<String, String>> metadataFiles = List.of(
                Map.entry("com/github/fge/msg-simple/maven-metadata.xml", "com/github/fge/"),
                Map.entry("org/jboss/eap/jboss-eap-parent/maven-metadata.xml", "org/jboss/eap/"),
                Map.entry("org/apache/lucene/lucene-core/maven-metadata.xml", "org/apache/lucene/"),
                Map.entry("org/wildfly/security/wildfly-elytron/maven-metadata.xml", "org/wildfly/security/"),
                Map.entry("io/dropwizard/metrics/metrics-core/maven-metadata.xml", "io/dropwizard/metrics/"),
                Map.entry("org/jboss/spec/javax/el/jboss-el-api_3.0_spec/maven-metadata.xml", "org/jboss/spec/javax/el/")
        );

        for (var entry : metadataFiles)
        {
            assertEquals( entry.getValue(), PathMaskChecker.extractGroupIdPath(entry.getKey()) );
        }
    }

    @Test
    public void checkListingMask() throws Exception
    {
        HostedRepository hostedRepository = new HostedRepository(
                "mvn-hosted"
        );

        RemoteRepository mrrcRepo = new RemoteRepository(
                "mrrc-ga",
                "http://example.url"
        );
        mrrcRepo.setPathMaskPatterns(
                Set.of("r|.+[-.]redhat[-_]\\d+.*|")
        );

        RemoteRepository repositoryMatched = new RemoteRepository(
                "koji-org.infinispan-infinispan-parent-9.4.2.Final_redhat_00001-2",
                "http://example.url"
        );
        repositoryMatched.setPathMaskPatterns(
                Set.of("r|org\\/infinispan\\/.+\\/9.4.2.Final-redhat-00001\\/.+|",
                        "org/infinispan/infinispan-query-dsl/maven-metadata.xml",
                        "r|org\\/infinispan\\/server\\/.+\\/9.4.2.Final-redhat-00001\\/.+|"));

        RemoteRepository repositoryUnMatched = new RemoteRepository(
                "koji-org.jboss.eap-jboss-eap-parent-7.2.0.GA_redhat_00002-2",
                "http://example.url"
        );
        repositoryUnMatched.setPathMaskPatterns(
                Set.of("r|org\\/jboss\\/eap\\/.+\\/7.2.0.GA-redhat-00002\\/.+|",
                        "org/jboss/eap/jboss-eap-parent/maven-metadata.xml"));

        RemoteRepository repositoryWithMultipleGroupIds = new RemoteRepository(
                "koji-com.sun.mail-all-1.6.1.redhat_1-1",
                "http://example.url"
        );
        repositoryWithMultipleGroupIds.setPathMaskPatterns(
                Set.of("javax/mail/javax.mail-api/maven-metadata.xml",
                        "com/sun/mail/javax.mail-api/maven-metadata.xml",
                        "r|javax\\/mail\\/.+\\/1.6.1.redhat-1\\/.+|",
                        "r|com\\/sun\\/mail\\/.+\\/1.6.1.redhat-1\\/.+|"));

        assertTrue(PathMaskChecker.checkListingMask(repositoryMatched, "org/"));
        assertTrue(PathMaskChecker.checkListingMask(repositoryMatched, "org/infinispan/"));
        assertTrue(PathMaskChecker.checkListingMask(repositoryMatched, "org/infinispan/infinispan-component-annotations/"));
        assertTrue(PathMaskChecker.checkListingMask(repositoryWithMultipleGroupIds, "com/sun/mail/"));
        assertTrue(PathMaskChecker.checkListingMask(repositoryWithMultipleGroupIds, "javax/mail/"));
        assertFalse(PathMaskChecker.checkListingMask(repositoryUnMatched, "org/infinispan/"));

        assertTrue(PathMaskChecker.checkListingMask(hostedRepository, "org/infinispan/"));
        assertTrue(PathMaskChecker.checkListingMask(mrrcRepo, "org/infinispan/"));
    }

}
