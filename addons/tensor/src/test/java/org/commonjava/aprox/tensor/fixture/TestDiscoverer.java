package org.commonjava.aprox.tensor.fixture;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;

import org.commonjava.aprox.inject.TestData;
import org.commonjava.aprox.tensor.discover.AproxProjectGraphDiscoverer;

@ApplicationScoped
@Default
@TestData
public class TestDiscoverer
    extends AproxProjectGraphDiscoverer
{

}
