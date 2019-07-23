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
package org.commonjava.indy.promote.model;

import io.swagger.annotations.ApiModelProperty;

/**
 * Contains the result of a promotion attempt. If the promotion is a success, the pending paths and error will be <b>null</b>. Otherwise, these are
 * populated to support the resume feature (for transient or correctable errors).
 *
 * @author jdcasey
 *
 */
public class GroupPromoteResult extends AbstractPromoteResult<GroupPromoteResult>
{

    @ApiModelProperty( "Original request" )
    private GroupPromoteRequest request;

    public GroupPromoteResult()
    {
    }

    public GroupPromoteResult( final GroupPromoteRequest request, final String error )
    {
        super( error, null );
        this.request = request;
        this.error = error;
    }

    public GroupPromoteResult( GroupPromoteRequest request, ValidationResult validations )
    {
        super( null, validations );
        this.request = request;
    }

    public GroupPromoteResult( GroupPromoteRequest request )
    {
        this.request = request;
    }

    public GroupPromoteRequest getRequest()
    {
        return request;
    }

    public void setRequest( final GroupPromoteRequest request )
    {
        this.request = request;
    }

    @Override
    public String toString()
    {
        return String.format( "GroupPromoteResult [\n  request=%s\n  error=%s\n  validations:\n  %s\n]", request,
                              getError(), getValidations() );
    }

}
