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
package org.commonjava.indy.conf;

import java.util.Properties;

/**
 * After the IndyConfigFactory loads configuration from disk into an IndyConfigInfo instance, if that instance implements
 * SystemPropertyProvider the config factory will query it for properties to be added.
 *
 * Created by jdcasey on 3/10/16.
 */
public interface SystemPropertyProvider
{
    Properties getSystemPropertyAdditions();
}
