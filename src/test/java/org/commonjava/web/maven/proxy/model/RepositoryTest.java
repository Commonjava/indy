package org.commonjava.web.maven.proxy.model;

import static org.commonjava.couch.util.IdUtils.namespaceId;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.couch.model.DenormalizationException;
import org.junit.Test;

public class RepositoryTest
{

    @Test
    public void denormalizeCouchDocumentIdFromName()
        throws DenormalizationException
    {
        Repository repo = new Repository( "test", "http://www.nowhere.com/" );
        repo.calculateDenormalizedFields();

        assertThat( repo.getCouchDocId(), equalTo( namespaceId( Repository.NAMESPACE, "test" ) ) );
    }

    @Test
    public void denormalizeAuthInfoFromUrl()
        throws DenormalizationException
    {
        Repository repo = new Repository( "test", "http://admin:admin123@www.nowhere.com/" );
        repo.calculateDenormalizedFields();

        assertThat( repo.getUrl(), equalTo( "http://www.nowhere.com/" ) );
        assertThat( repo.getUser(), equalTo( "admin" ) );
        assertThat( repo.getPassword(), equalTo( "admin123" ) );
    }

}
