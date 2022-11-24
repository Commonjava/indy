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
package org.commonjava.indy.client.core.metric;

public class ClientMetricConstants {

    public static final String CLIENT_FOLO_ADMIN = "client.folo.admin";

    public static final String CLIENT_FOLO_CONTENT = "client.folo.content";

    public static final String CLIENT_REPO_MGMT = "client.repo.mgmt";

    public static final String CLIENT_CONTENT = "client.content";

    public static final String CLIENT_PROMOTE = "client.promote";

    public final static String HEADER_CLIENT_API = "Indy-Client-API";

    public final static String HEADER_CLIENT_TRACE_ID = "Indy-Client-Trace-Id";

    public final static String HEADER_CLIENT_SPAN_ID = "Indy-Client-Span-Id";

    public static final String[] CLIENT_FUNCTIONS =
            { CLIENT_FOLO_ADMIN, CLIENT_FOLO_CONTENT, CLIENT_REPO_MGMT, CLIENT_CONTENT, CLIENT_PROMOTE };

    private ClientMetricConstants() {
    }
}
