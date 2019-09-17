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
package org.commonjava.indy.metrics.healthcheck.impl;

import org.commonjava.indy.metrics.healthcheck.IndyComponentHC;
import org.commonjava.indy.metrics.healthcheck.IndyHealthCheck;

import javax.inject.Named;

@Named
public class DeadlockHealthCheck
        extends IndyComponentHC
{
    com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck
                    deadlock = new com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck();

    @Override
    protected Result check() throws Exception
    {
        return deadlock.execute();
    }

}
