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
 * Created by ruhan on 12/5/18.
 */
public abstract class AbstractPromoteResult<T extends AbstractPromoteResult>
{
    public static final String DONE = "DONE";

    public static final String ACCEPTED = "ACCEPTED";

    @ApiModelProperty( "Result code" )
    protected String resultCode = DONE; // default

    @ApiModelProperty( "Result of validation rule executions, if applicable" )
    protected ValidationResult validations;

    @ApiModelProperty( "Error message, if promotion failed" )
    protected String error;

    protected AbstractPromoteResult(){}

    protected AbstractPromoteResult( final String error, final ValidationResult validations )
    {
        this.validations = validations;
        if ( error != null )
        {
            this.error = error;
        }
        else
        {
            this.error = ( validations == null || validations.isValid() ) ? null : "Promotion validation failed";
        }
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

    public ValidationResult getValidations()
    {
        return validations;
    }

    public void setValidations( ValidationResult validations )
    {
        this.validations = validations;
    }
}
