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
package org.commonjava.indy.conf;


/**
 * Registration helper to describe configuration for an add-on or subsystem of Indy. This helps Indy keep configurations local to the
 * codebases that use them, by allowing them to register enough information to hook into the {@link IndyConfigFactory} and read a subsection
 * of the overall configuration (file).
 * 
 */
public interface IndyConfigClassInfo
    extends IndyConfigInfo
{

    /**
     * The configuration class which will be instantiated with information read by the {@link IndyConfigFactory}.
     */
    Class<?> getConfigurationClass();

}
