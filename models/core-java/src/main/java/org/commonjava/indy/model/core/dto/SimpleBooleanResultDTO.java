/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.model.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel( "Representation of a simple boolean result of query, like if the stores data is empty" )
public class SimpleBooleanResultDTO
{
    @JsonProperty
    @ApiModelProperty( required = true, value = "The description of this boolean result" )
    private String description;

    @JsonProperty
    @ApiModelProperty( required = true, value = "The boolean result" )
    private Boolean result;

    public String getDescription()
    {
        return description;
    }

    public void setDescription( String description )
    {
        this.description = description;
    }

    public Boolean getResult()
    {
        return result;
    }

    public void setResult( Boolean result )
    {
        this.result = result;
    }
}