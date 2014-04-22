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
package org.commonjava.aprox.depgraph.fixture;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import org.commonjava.aprox.depgraph.discover.AproxProjectGraphDiscoverer;
import org.commonjava.aprox.inject.TestData;

@ApplicationScoped
@Default
@TestData
public class TestDiscoverer
    extends AproxProjectGraphDiscoverer
{

}
