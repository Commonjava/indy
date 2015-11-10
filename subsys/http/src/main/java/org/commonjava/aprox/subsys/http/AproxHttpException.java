/**
 * Copyright (C) 2011 Red Hat, Inc. (jdcasey@commonjava.org)
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
package org.commonjava.aprox.subsys.http;

import org.commonjava.aprox.AproxException;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.text.MessageFormat;

/**
 * Signals an error to initialize an http client instance (<b>not</b> for content transfers).
 */
public class AproxHttpException
    extends AproxException
{
    public AproxHttpException( final String message, final Object... params )
    {
        super( message, params );
    }

    public AproxHttpException( final String message, final Throwable cause, final Object... params )
    {
        super( message, cause, params );
    }

    private static final long serialVersionUID = 1L;

}
