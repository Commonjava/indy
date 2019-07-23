/**
 * Copyright (C) 2011-2019 Red Hat, Inc. (https://github.com/Commonjava/indy)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.model.spi;

import io.swagger.annotations.ApiModel;

import java.util.Collections;
import java.util.List;

/**
 * DTO used to wrap a listing of available add-ons installed in the system. Wrapper embeds these id's in an "items" list, to work around a known
 * JSON security flaw.
 * <br/>
 * See: <a href="http://stackoverflow.com/questions/3503102/what-are-top-level-json-arrays-and-why-are-they-a-security-risk">
 * http://stackoverflow.com/questions/3503102/what-are-top-level-json-arrays-and-why-are-they-a-security-risk
 * </a>
 */
@ApiModel( description = "Listing of add-ons available on the system, with metadata" )
public class AddOnListing
{

    private List<IndyAddOnID> items;

    public AddOnListing()
    {
    }

    public AddOnListing( final List<IndyAddOnID> addOnNames )
    {
        this.items = addOnNames;
        Collections.sort( items );
    }

    public List<IndyAddOnID> getItems()
    {
        return items;
    }

    public void setItems( final List<IndyAddOnID> items )
    {
        this.items = items;
    }

}
