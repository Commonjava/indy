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

    @ApiModelProperty( "Result of validation rule executions, if applicable" )
    private ValidationResult validations;

    @ApiModelProperty( "Error message, if promotion failed" )
    private String error;

    public GroupPromoteResult()
    {
    }

    public GroupPromoteResult( final GroupPromoteRequest request, final String error )
    {
        this.request = request;
        this.error = error;
    }

    public GroupPromoteResult( GroupPromoteRequest request, ValidationResult validations )
    {
        this.request = request;
        this.validations = validations;
    }

    public GroupPromoteResult( GroupPromoteRequest request )
    {
        this.request = request;
    }

    public boolean succeeded()
    {
        return error == null && ( validations == null || validations.isValid() );
    }

    public String getError()
    {
        return error;
    }

    public void setError( final String error )
    {
        this.error = error;
    }

    public GroupPromoteRequest getRequest()
    {
        return request;
    }

    public void setRequest( final GroupPromoteRequest request )
    {
        this.request = request;
    }

    public ValidationResult getValidations()
    {
        return validations;
    }

    public void setValidations( ValidationResult validations )
    {
        this.validations = validations;
    }

    @Override
    public String toString()
    {
        return String.format( "GroupPromoteResult [\n  request=%s\n  error=%s\n  validations:\n  %s\n]", request, error, validations );
    }

}
