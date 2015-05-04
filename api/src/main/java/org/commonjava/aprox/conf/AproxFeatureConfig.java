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
package org.commonjava.aprox.conf;

/**
 * Describes a CDI-style Provider that @Produces an {@link AproxConfigClassInfo}, for registration in the configuration reader driven by 
 * {@link AproxConfigFactory}. The configuration class, implementation of {@link AproxConfigClassInfo}, and this class form a necessary triple-class 
 * mechanism necessary to auto-register add-on and subsystem configurations flexibly into a single configuration reader/factory.
 * 
 * @author jdcasey
 *
 */
public interface AproxFeatureConfig
{

    /**
     * Construct (if necessary) and return an {@link AproxConfigClassInfo} that can be registered to guide the creation/population of the specific 
     * configuration class we're interested in.
     */
    AproxConfigClassInfo getInfo();

}
