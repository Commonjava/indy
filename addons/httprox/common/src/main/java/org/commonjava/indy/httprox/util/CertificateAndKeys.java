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
package org.commonjava.indy.httprox.util;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

/**
 * Created by ruhan on 9/18/18.
 */
public class CertificateAndKeys
{
    private final Certificate certificate;

    private final PublicKey publicKey;

    private final PrivateKey privateKey;

    public CertificateAndKeys( Certificate certificate, KeyPair keyPair )
    {
        this.certificate = certificate;
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public CertificateAndKeys( Certificate certificate, PrivateKey privateKey, PublicKey publicKey )
    {
        this.certificate = certificate;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
    }

    public Certificate getCertificate()
    {
        return certificate;
    }

    public PrivateKey getPrivateKey()
    {
        return privateKey;
    }

    public PublicKey getPublicKey()
    {
        return publicKey;
    }
}
