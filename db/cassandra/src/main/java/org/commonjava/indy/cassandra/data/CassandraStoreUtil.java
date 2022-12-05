/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.cassandra.data;

import org.commonjava.indy.core.conf.IndyStoreManagerConfig;

public class CassandraStoreUtil
{

    public static final String TABLE_STORE = "artifactstore";
    public static final String TABLE_AFFECTED_STORE = "affected_store";

    public static final String PACKAGE_TYPE = "packageType";
    public static final String STORE_TYPE = "storeType";
    public static final String NAME_HASH_PREFIX = "nameHashPrefix";
    public static final String NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String DISABLED = "disabled";
    public static final String PATH_STYLE = "pathStyle";
    public static final String TRANSIENT_METADATA = "transientMetadata";
    public static final String METADATA = "metadata";
    public static final String PATH_MASK_PATTERNS = "pathMaskPatterns";
    public static final String CREATE_TIME = "createTime";
    public static final String DISABLE_TIMEOUT = "disableTimeout";
    public static final String RESCAN_IN_PROGRESS = "rescanInProgress";
    public static final String AUTHORITATIVE_INDEX = "authoritativeIndex";
    public static final String EXTRAS = "extras";

    // the attributes of abstract repository
    public static final String ALLOW_SNAPSHOTS = "allowSnapshots";
    public static final String ALLOW_RELEASES = "allowReleases";

    // the attributes of remote repository
    public static final String URL = "url";
    public static final String HOST = "host";
    public static final String PORT = "port";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String PROXY_USER = "proxyUser";
    public static final String PROXY_PASSWORD = "proxyPassword";
    public static final String KEY_CERT_PEM = "keyCertPem";
    public static final String KEY_PASSWORD = "keyPassword";
    public static final String TIMEOUT_SECONDS = "timeoutSeconds";
    public static final String CACHE_TIMEOUT_SECONDS = "cacheTimeoutSeconds";
    public static final String METADATA_TIMEOUT_SECONDS = "metadataTimeoutSeconds";
    public static final String NFC_TIMEOUT_SECONDS = "nfcTimeoutSeconds";
    public static final String PREFETCH_RESCAN = "prefetchRescan";
    public static final String PREFETCH_RESCAN_TIMESTAMP = "prefetchRescanTimestamp";
    public static final String MAX_CONNECTIONS = "maxConnections";
    public static final String SERVER_CERT_PEM = "serverCertPem";
    public static final String PASS_THROUGH = "passThrough";
    public static final String IGNORE_HOST_NAME_VERIFICATION = "ignoreHostnameVerification";

    // the attributes of hosted repository
    public static final String STORAGE = "storage";
    public static final String READONLY = "readonly";
    public static final String SNAPSHOT_TIMEOUT_SECONDS = "snapshotTimeoutSeconds";

    // the attributes of group repository
    public static final String CONSTITUENTS = "constituents";
    public static final String PREPEND_CONSTITUENT = "prependConstituent";

    public static final String KEY = "key";
    public static final String AFFECTED_STORES = "affectedStores";

    public static final int MODULO_VALUE = 10;

    public static String getSchemaCreateTableStore( String keySpace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keySpace + "." + TABLE_STORE + " ("
                        + "typekey varchar,"
                        + "namehashprefix int,"
                        + "packagetype varchar,"
                        + "storetype varchar,"
                        + "name varchar,"
                        + "description varchar,"
                        + "disabled boolean,"
                        + "pathstyle varchar,"
                        + "transientmetadata map<text, text>,"
                        + "metadata map<text, text>,"
                        + "pathmaskpatterns set<text>,"
                        + "createtime varchar,"
                        + "disabletimeout int,"
                        + "authoritativeindex boolean,"
                        + "rescaninprogress boolean,"
                        + "extras map<text, text>,"
                        + "PRIMARY KEY (( typekey, namehashprefix ), name )"
                        + ");";
    }

    public static String getSchemaCreateTableAffectedStore( String keyspace )
    {
        return "CREATE TABLE IF NOT EXISTS " + keyspace + "." + TABLE_AFFECTED_STORE + " ("
                        + "key varchar,"
                        + "affectedStores set<text>,"
                        + "PRIMARY KEY ( key )"
                        + ");";
    }

    public static String getSchemaCreateIndex4Store( String keyspace )
    {
        return "CREATE INDEX IF NOT EXISTS typekey_idx on " + keyspace + "." + TABLE_STORE + " (typekey)";
    }

    public static int getHashPrefix( final String name )
    {
        String prefix = name.length() > 12 ? name.substring( 0, 12 ) : name;
        // Sometimes it returns the negative value from hashCode(), let's use a shift mask to handle it
        return (prefix.hashCode() & 0x7fffffff) % MODULO_VALUE;
    }

    public static String getTypeKey( final String packageType, final String storeType )
    {
        return packageType + ":" + storeType;
    }
}
