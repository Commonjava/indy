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
package org.commonjava.indy.client.core.metric;

import org.commonjava.indy.client.core.inject.ClientMetricSet;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsFunctionMetrics;
import org.commonjava.o11yphant.metrics.sli.GoldenSignalsMetricSet;

import java.util.Arrays;
import java.util.Collection;

import static org.commonjava.indy.client.core.metric.ClientMetricConstants.CLIENT_FUNCTIONS;

@ClientMetricSet
public class ClientGoldenSignalsMetricSet
        extends GoldenSignalsMetricSet {

    public ClientGoldenSignalsMetricSet() {
        super();
    }

    @Override
    protected Collection<String> getFunctions() {
        return Arrays.asList( CLIENT_FUNCTIONS );
    }

    public void clear()
    {
        getFunctionMetrics().clear();
        getFunctions().forEach( function -> {
            getFunctionMetrics().put( function, new GoldenSignalsFunctionMetrics( function ) );
        } );
    }

}