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
