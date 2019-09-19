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
package org.commonjava.indy.promote.model;

import org.commonjava.indy.model.core.StoreKey;

/**
 * Created by jdcasey on 9/11/15.
 */
public interface PromoteRequest<T extends PromoteRequest<T>>
{
    StoreKey getSource();

    T setSource( StoreKey source );

    StoreKey getTargetKey();

    boolean isDryRun();

    T setDryRun( boolean dryRun );

    boolean isFireEvents();

    boolean isAsync();

    CallbackTarget getCallback();
}
