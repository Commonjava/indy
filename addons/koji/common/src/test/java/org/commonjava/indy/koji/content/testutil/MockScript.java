package org.commonjava.indy.koji.content.testutil;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by jdcasey on 2/10/17.
 */
public class MockScript
{
    public static final String MOCK_SCRIPT_JSON = "mock-script.json";

    private List<String> scriptOrder;

    private transient AtomicInteger counter;

    public List<String> getScriptOrder()
    {
        return scriptOrder;
    }

    public void setScriptOrder( List<String> scriptOrder )
    {
        this.scriptOrder = scriptOrder;
    }

    public String getNextScriptBaseName()
    {
        if ( counter == null )
        {
            throw new RuntimeException( "Counter not set!" );
        }

        int next = counter.getAndIncrement();
        if ( next >= scriptOrder.size() )
        {
            return null;
        }

        return scriptOrder.get( next );
    }

    public int getHumanReadableScriptAttemptCount()
    {
        return counter.get()+1;
    }

    public int getScriptCount()
    {
        return scriptOrder.size();
    }

    public void setCounter( AtomicInteger exchangeCounter )
    {
        this.counter = exchangeCounter;
    }
}
