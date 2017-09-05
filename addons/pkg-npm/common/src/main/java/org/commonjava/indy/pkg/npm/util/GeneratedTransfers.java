/**
 * Copyright (C) 2017 Red Hat, Inc. (yma@commonjava.org)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.commonjava.indy.pkg.npm.util;

import org.commonjava.maven.galley.model.Transfer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeneratedTransfers
{

    private static Map<Transfer, List<Transfer>> map = new HashMap<>();

    public static void setGeneratedTransfers( Transfer target, Transfer... generated )
    {
        List<Transfer> list = new ArrayList<>();
        for ( Transfer t : generated )
        {
            list.add( t );
        }
        map.put( target, list );
    }

    public static List<Transfer> getGeneratedTransfers( Transfer target )
    {
        return map.get( target );
    }

}
