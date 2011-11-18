/*******************************************************************************
 * Copyright (C) 2011  John Casey
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see 
 * <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.commonjava.aprox.core.model;

import static org.commonjava.couch.util.IdUtils.namespaceId;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.commonjava.aprox.core.model.Repository;
import org.commonjava.aprox.core.model.StoreType;
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

        assertThat( repo.getCouchDocId(),
                    equalTo( namespaceId( StoreType.repository.name(), "test" ) ) );
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
