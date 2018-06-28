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
package org.commonjava.indy.bind.jaxrs;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import static org.commonjava.indy.IndyRequestConstants.HEADER_COMPONENT_ID;

/**
 * The scope annotations (Thread, Header, MDC) tell where the constant is available/used.
 */
public class RequestContextConstants
{
    // Scope annotations

    @Target( { FIELD } )
    @Retention( SOURCE )
    private @interface Header{}

    @Target( { FIELD } )
    @Retention( SOURCE )
    private @interface MDC{}

    @Target( { FIELD } )
    @Retention( SOURCE )
    private @interface Thread{}

    //

    @Header @MDC
    public static final String COMPONENT_ID = HEADER_COMPONENT_ID; // "component-id";

    @Header
    public static final String X_FORWARDED_FOR = "x-forwarded-for";

    @Header @Thread @MDC
    public static final String EXTERNAL_ID = "external-id";

    @Thread @MDC
    public static final String CLIENT_ADDR = "client-addr";

    @Thread @MDC
    public static final String INTERNAL_ID = "internal-id";

    @Thread @MDC
    public static final String PREFERRED_ID = "preferred-id";

}
