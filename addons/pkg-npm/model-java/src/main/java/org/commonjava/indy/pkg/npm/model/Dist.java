/**
 * Copyright (C) 2011-2023 Red Hat, Inc. (https://github.com/Commonjava/indy)
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

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class Dist
{

    private final String shasum;

    private final String tarball;

    private final String integrity;

    private final Integer fileCount;

    private final Long unpackedSize;

    private final List<Map<String, String>> signatures;

    @JsonProperty( "npm-signature" )
    private final String npmSignature;

    protected Dist()
    {
        this.shasum = null;
        this.tarball = null;
        this.integrity = null;
        this.fileCount = null;
        this.unpackedSize = null;
        this.signatures = null;
        this.npmSignature = null;
    }
    public Dist( final String tarball )
    {
        this.tarball = tarball;
        this.shasum = null;
        this.integrity = null;
        this.fileCount = null;
        this.unpackedSize = null;
        this.signatures = null;
        this.npmSignature = null;
    }

    public Dist( final String tarball, final String shasum, final String integrity, final Integer fileCount, final Long unpackedSize, final List<Map<String, String>> signatures, final String npmSignature )
    {
        this.tarball = tarball;
        this.shasum = shasum;
        this.integrity = integrity;
        this.fileCount = fileCount;
        this.unpackedSize = unpackedSize;
        this.signatures = signatures;
        this.npmSignature = npmSignature;
    }

    public String getShasum()
    {
        return shasum;
    }

    public String getTarball()
    {
        return tarball;
    }

    public String getIntegrity()
    {
        return integrity;
    }

    public Integer getFileCount()
    {
        return fileCount;
    }

    public Long getUnpackedSize()
    {
        return unpackedSize;
    }

    public List<Map<String, String>> getSignatures()
    {
        return signatures;
    }

    public String getNpmSignature()
    {
        return npmSignature;
    }

}
