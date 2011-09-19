package org.commonjava.web.maven.proxy.model;

import static org.commonjava.couch.util.IdUtils.namespaceId;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.couch.model.DenormalizationException;
import org.junit.Test;

public class GroupTest
{

    @Test
    public void denormalizeCouchDocumentIdFromName()
        throws DenormalizationException
    {
        Group grp = new Group( "test" );
        grp.calculateDenormalizedFields();

        assertThat( grp.getCouchDocId(), equalTo( namespaceId( Group.NAMESPACE, "test" ) ) );
    }

}
