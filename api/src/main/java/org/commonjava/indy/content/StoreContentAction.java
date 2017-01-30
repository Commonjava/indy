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
package org.commonjava.indy.content;

import org.commonjava.indy.model.core.Group;
import org.commonjava.indy.model.core.StoreKey;

import java.util.Set;

/**
 * Created by jdcasey on 1/27/17.
 */
public interface StoreContentAction
{
    void clearStoreContent( Set<String> paths, StoreKey originKey, Set<Group> affectedGroups, boolean clearOriginPath );
}
