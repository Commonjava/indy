package org.commonjava.indy.subsys.metrics;

import org.commonjava.o11yphant.metrics.sli.GoldenSignalsMetricSet;

import javax.enterprise.context.ApplicationScoped;
import java.util.Arrays;
import java.util.Collection;

import static org.commonjava.indy.subsys.metrics.IndyTrafficClassifierConstants.FUNCTIONS;

@ApplicationScoped
public class IndyGoldenSignalsMetricSet
        extends GoldenSignalsMetricSet
{
    @Override
    protected Collection<String> getFunctions()
    {
        return Arrays.asList( FUNCTIONS );
    }

}
