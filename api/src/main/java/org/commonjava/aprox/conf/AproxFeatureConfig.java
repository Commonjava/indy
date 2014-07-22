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
 * Describes a CDI-style Provider that @Produces an {@link AproxConfigInfo}, for registration in the configuration reader driven by 
 * {@link AproxConfigFactory}. The configuration class, implementation of {@link AproxConfigInfo}, and this class form a necessary triple-class 
 * mechanism necessary to auto-register add-on and subsystem configurations flexibly into a single configuration reader/factory.
 * 
 * @author jdcasey
 *
 */
public interface AproxFeatureConfig
{

    /**
     * Construct (if necessary) and return an {@link AproxConfigInfo} that can be registered to guide the creation/population of the specific 
     * configuration class we're interested in.
     */
    AproxConfigInfo getInfo();

}
