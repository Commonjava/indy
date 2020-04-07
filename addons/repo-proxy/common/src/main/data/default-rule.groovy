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

import org.commonjava.indy.repo.proxy.create.*

import org.commonjava.indy.model.core.*;

class DefaultRule extends AbstractProxyRepoCreateRule {
    @Override
    boolean matches(StoreKey storeKey) {
        return "maven".equals(storeKey.getPackageType()) && (StoreType.group == storeKey.getType() || StoreType.hosted == storeKey.getType())
    }

    @Override
    RemoteRepository createRemote(StoreKey key) {
        def pkgType = key.getPackageType()
        def type = key.getType().singularEndpointName()
        def name = key.getName()
        return new RemoteRepository(key.getPackageType(), key.getName(), String.format("http://some.indy/api/content/%s/%s/%s", pkgType, type, name))
    }

}
