/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.core.model;

public interface Repository
    extends ArtifactStore
{
    String getUrl();

    void setUrl( final String url );

    String getUser();

    void setUser( final String user );

    String getPassword();

    void setPassword( final String password );

    String getHost();

    int getPort();

    int getTimeoutSeconds();

    void setTimeoutSeconds( final int timeoutSeconds );

    int getCacheTimeoutSeconds();

    void setCacheTimeoutSeconds( final int cacheTimeoutSeconds );

    boolean isCached();

    void setCached( boolean cached );

}
