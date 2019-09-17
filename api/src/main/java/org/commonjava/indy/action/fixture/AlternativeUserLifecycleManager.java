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
package org.commonjava.indy.action.fixture;

import org.commonjava.indy.action.IndyLifecycleAction;
import org.commonjava.indy.action.UserLifecycleManager;

import javax.enterprise.inject.Alternative;
import java.util.Collection;
import java.util.Collections;

/**
 * The default implementation for UserLifecycleManager is in core. For those who use indy-api but not indy-core,
 * this can be an alternative injection class via beans.xml.
 *
 * Created by ruhan on 11/16/16.
 */
@Alternative
public final class AlternativeUserLifecycleManager
        implements UserLifecycleManager
{
    public <T extends IndyLifecycleAction> Collection<T> getUserLifecycleActions(String lifecycleName, Class<T> type)
    {
        return Collections.EMPTY_SET;
    }
}
