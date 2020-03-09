/**
 * Copyright (C) 2011-2020 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.httprox;

import static org.commonjava.indy.httprox.util.CertUtils.MILLIS_IN_DAY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.CertException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CRLConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.commonjava.indy.httprox.util.CertUtils;
import org.commonjava.indy.httprox.util.CertificateAndKeys;

import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertUtilsTest {

    private KeyPairGenerator keyPairGenerator;
    private KeyPair pair;
    private PrivateKey caKey;
    private X509Certificate caCert;
    private static final Logger logger = LoggerFactory.getLogger( CertUtilsTest.class );

    @Test
    public void testCertificateNotNull()
        throws Exception
    {
        assertNotNull( keyPairGenerator );
        assertNotNull( pair );
        String subjectCN = "CN=testcase.org, O=Test Org";
        CertificateAndKeys certAndKeys = CertUtils.createSignedCertificateAndKey( subjectCN, caCert, caKey, false );
        assertNotNull( certAndKeys.getCertificate() );
    }

    @Test
    public void testCertificateValid()
        throws Exception
    {
        String subjectCN = "CN=testcase.org, O=Test Org";
        CertificateAndKeys certAndKeys = CertUtils.createSignedCertificateAndKey( subjectCN, caCert, caKey, false );
        Certificate cert = certAndKeys.getCertificate();
        PublicKey pubKey = caCert.getPublicKey();
        cert.verify( pubKey, BouncyCastleProvider.PROVIDER_NAME );
    }

    @Test (expected = CertificateNotYetValidException.class)
    public void testCertificateNotYetValid()
        throws Exception
    {
        String subjectCN = "CN=testcase.org, O=Test Org";
        CertificateAndKeys certAndKeys = CertUtils.createSignedCertificateAndKey( subjectCN, caCert,caKey, false );
        GregorianCalendar yesterday = new GregorianCalendar();
        yesterday.add( Calendar.DAY_OF_WEEK, -1 );
        X509Certificate cert = (X509Certificate) certAndKeys.getCertificate();
        cert.checkValidity( yesterday.getTime() );
    }

    @Test
    public void testSubjectCertificateSignedByIssuerCertificateWithoutExtensionIsValid()
        throws Exception, CertificateException, OperatorCreationException, CertificateEncodingException, CertException
    {
        String subjectCN = "CN=testcase.org, O=Test Org";
        CertificateAndKeys certificateAndKeys = CertUtils.createSignedCertificateAndKey( subjectCN, caCert, caKey, false );
        PublicKey publicKey = certificateAndKeys.getPublicKey();
        X509CertificateHolder certHolder = new X509CertificateHolder( certificateAndKeys.getCertificate().getEncoded() );
        JcaContentVerifierProviderBuilder verifierBuilder = new JcaContentVerifierProviderBuilder().setProvider( BouncyCastleProvider.PROVIDER_NAME );
        assertTrue( certHolder.isSignatureValid( verifierBuilder.build( caCert ) ) );
        Extension ext = certHolder.getExtension( X509Extension.basicConstraints );
        assertNotNull( ext );
    }

    @Test
    public void testIntermediateSignedCertificateWithExtension()
        throws Exception, CertificateException, OperatorCreationException, CertificateEncodingException, CertException
    {
        String subjectCN = "CN=testcase.org, O=Test Org";
        CertificateAndKeys certificateAndKeys = CertUtils.createSignedCertificateAndKey( subjectCN, caCert, caKey, true );
        PublicKey publicKey = certificateAndKeys.getPublicKey();
        X509CertificateHolder certHolder = new X509CertificateHolder( certificateAndKeys.getCertificate().getEncoded() );
        Extension ext = certHolder.getExtension( Extension.basicConstraints );
        assertNotNull( ext );
    }

    @Before
    public void setUp()
        throws NoSuchAlgorithmException, Exception, CertificateException
    {
        keyPairGenerator = KeyPairGenerator.getInstance( "RSA" );
        pair = keyPairGenerator.generateKeyPair();
        caKey = CertUtils.getPrivateKey( "src/test/resources/ca.der" );
        caCert = CertUtils.loadX509Certificate( new File("src/test/resources", "ca.crt") );
    }
}
