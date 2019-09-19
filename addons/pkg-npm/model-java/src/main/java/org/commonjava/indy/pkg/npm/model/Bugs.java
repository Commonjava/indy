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
package org.commonjava.indy.pkg.npm.model;

import io.swagger.annotations.ApiModelProperty;

public class Bugs
{

    @ApiModelProperty( value = "Url to the project's issue tracker." )
    private final String url;

    @ApiModelProperty( value = "The email address to which issues should be reported." )
    private final String email;

    protected Bugs()
    {
        this.url = null;
        this.email = null;
    }

    public Bugs( final String url, final String email )
    {
        this.url = url;
        this.email = email;
    }

    public String getUrl()
    {
        return url;
    }

    public String getEmail()
    {
        return email;
    }

}
