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
package org.commonjava.indy.pkg.npm.model;

import io.swagger.annotations.ApiModel;

import java.util.HashMap;
import java.util.Map;

@ApiModel( description = "Package distribution tags, which can be used to provide an alias instead of version numbers, different tags are for the multi project development streams." )
public class DistTag
{
    public static final String LATEST = "latest";

    public static final String STABLE = "stable";

    public static final String BETA = "beta";

    public static final String DEV = "dev";

    private static final String CANARY = "canary";

    private Map<String, String> tagsMap = new HashMap<String, String>();

    public DistTag()
    {
    }

    public String getLatest() {
        return tagsMap.get(LATEST);
    }

    public void setLatest(String latest) {
        tagsMap.put(LATEST, latest);
    }

    public String getStable() {
        return tagsMap.get(STABLE);
    }

    public void setStable(String stable) {
        tagsMap.put(STABLE,stable);
    }

    public String getBeta() {
        return tagsMap.get(BETA);
    }

    public void setBeta(String beta) {
        tagsMap.put(BETA,beta);
    }

    public String getDev() {
        return tagsMap.get(DEV);
    }

    public void setDev(String dev) {
        tagsMap.put(DEV,dev);
    }

    public String getCanary()
    {
        return tagsMap.get( CANARY );
    }

    public void setCanary( String canary )
    {
        tagsMap.put( CANARY, canary );
    }

    public Map<String, String> fetchTagsMap()
    {
        return tagsMap;
    }

    public String getTag(String tag) {
        return tagsMap.get(tag);
    }

    public void putTag(String tag, String value) {
        tagsMap.put(tag, value);
    }
}
