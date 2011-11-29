/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
