/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.pkg.npm.model.converter;

import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.HashMap;
import java.util.Map;

public class ObjectToBinConverter
        extends StdConverter<Object, Map<String, String>>
{

    public static final String SINGLE_BIN = "SINGLE_BIN";

    @Override
    public Map<String, String> convert( Object o )
    {
        if ( o instanceof Map )
        {
            return (Map<String, String>) o;
        }
        // Use SPDX expressions, ref https://docs.npmjs.com/cli/v7/configuring-npm/package-json
        // parse String value to Map value
        Map<String, String> result = new HashMap<>();
        result.put( SINGLE_BIN, o.toString() );
        return result;
    }
}
