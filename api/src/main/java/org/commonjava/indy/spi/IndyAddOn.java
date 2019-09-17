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
package org.commonjava.indy.spi;

import org.commonjava.indy.model.spi.IndyAddOnID;

/**
 * Component that describes an add-on to Indy. This is currently most useful when the add-on provides UI elements, and is the only way to get new
 * parts added to the UI.
 */
public interface IndyAddOn
{

    /**
     * Retrieve an identifier for this add-on which contains various UI configurations, along with a basic name for the add-on.
     */
    IndyAddOnID getId();

}
