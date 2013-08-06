package org.commonjava.aprox.tensor.fixture;

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
