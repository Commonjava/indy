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
package org.commonjava.indy.subsys.infinispan.metrics;

import org.commonjava.indy.subsys.metrics.conf.IndyMetricsConfig;
import org.commonjava.indy.subsys.infinispan.CacheProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class IspnRegistrySetProvider
{
    @Inject
    private IndyMetricsConfig metricsConfig;

    @Inject
    private CacheProducer cacheProducer;

    @Inject
    private Instance<IspnCacheRegistry> cacheRegistrySet;

    private final Logger logger = LoggerFactory.getLogger( getClass() );

}
