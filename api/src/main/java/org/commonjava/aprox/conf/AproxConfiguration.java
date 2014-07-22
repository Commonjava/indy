/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.commonjava.aprox.conf;

/**
 * Base configuration for AProx's core. This basically sets timeouts for a couple different (relatively obscure) features.
 */
public interface AproxConfiguration
{

    /**
     * Number of seconds before the content in a repository marked as "pass-through" will timeout. Pass-through remote repos are designed
     * to hold content for a minimal amount of time. They would be a simple pass-through, non-caching proxy, but in reality artifacts are usually 
     * referenced more than once from any given build. So, setting a very low pass-through timeout for content in these repositories helps to
     * cut down on traffic to the upstream repository.
     */
    int getPassthroughTimeoutSeconds();

    /**
     * Number of seconds before a negative cache (or, not-found cache) URL record will timeout. These are used to prevent constant re-request for URLs
     * that have been known to fail for one reason or another (ranging from 404's to 500's, to just about anything else that's not HTTP 200, 301, or 
     * 302). This value can be tuned based on use case, but for most people it should be relatively high.
     * <br/>
     * Also, the NFC (Not-Found Cache) can be maintained manually via the UI.
     */
    int getNotFoundCacheTimeoutSeconds();

}
