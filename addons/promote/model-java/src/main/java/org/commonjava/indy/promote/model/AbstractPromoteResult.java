/**
 * Copyright (C) 2011-2018 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.promote.model;

import io.swagger.annotations.ApiModelProperty;

import java.util.UUID;

/**
 * Created by ruhan on 12/5/18.
 */
public class AbstractPromoteResult<T extends AbstractPromoteResult>
{
    public static final String DONE = "DONE";

    public static final String ACCEPTED = "ACCEPTED";

    @ApiModelProperty( "Unique promotion Id" )
    protected String promotionId = UUID.randomUUID().toString(); // default

    @ApiModelProperty( "Result code" )
    protected String resultCode = DONE; // default

    public String getPromotionId()
    {
        return promotionId;
    }

    public void setPromotionId( String promotionId )
    {
        this.promotionId = promotionId;
    }

    public String getResultCode()
    {
        return resultCode;
    }

    public void setResultCode( String resultCode )
    {
        this.resultCode = resultCode;
    }

    public T accepted()
    {
        this.resultCode = ACCEPTED;
        return (T) this;
    }

    public T withPromotionId( String promotionId )
    {
        this.promotionId = promotionId;
        return (T) this;
    }
}
