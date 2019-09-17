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
package org.commonjava.indy.subsys.keycloak.util;

import org.apache.commons.codec.binary.Hex;
import org.keycloak.common.util.Base64Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jdcasey on 9/1/15.
 */
public class KeycloakBearerTokenDebug
{

    public static void debugToken( String tokenString )
    {
        Logger logger = LoggerFactory.getLogger( KeycloakBearerTokenDebug.class );
        logger.debug( "Raw token:\n  '{}'", tokenString );

        String[] parts = tokenString.split("\\.");
        if (parts.length < 2 || parts.length > 3) throw new IllegalArgumentException("Parsing error");
        String encodedHeader = parts[0];
        String encodedContent = parts[1];
        try {
            logger.debug("Decoded content:\n  '{}'", new String( Base64Url.decode( encodedContent ) ) );
            if (parts.length > 2) {
                String encodedSignature = parts[2];
                byte[] sig = Base64Url.decode( encodedSignature );
                String sigStr = Hex.encodeHexString( sig );
                logger.debug( "Got signature {} bytes long:\n\n'{}'", sig.length, sigStr );
            }

            byte[] headerBytes = Base64Url.decode( encodedHeader );
            logger.debug( "Decoded header:\n  '{}'", new String( headerBytes ) );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
