/**
 * Copyright (C) 2011-2022 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.expire;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;

@Indexed
public class ScheduleValue
        implements Externalizable
{
    @Field( index = Index.YES, analyze = Analyze.NO )
    private ScheduleKey key;

    @Field( index = Index.NO, analyze = Analyze.NO )
    private Map<String, Object> dataPayload;

    public ScheduleValue()
    {
    }

    public ScheduleValue( ScheduleKey key, Map<String, Object> dataPayload )
    {
        this.key = key;
        this.dataPayload = dataPayload;
    }

    public ScheduleKey getKey()
    {
        return key;
    }

    public void setKey( ScheduleKey key )
    {
        this.key = key;
    }

    public Map<String, Object> getDataPayload()
    {
        return dataPayload;
    }

    public void setDataPayload( Map<String, Object> dataPayload )
    {
        this.dataPayload = dataPayload;
    }

    @Override
    public void writeExternal( ObjectOutput out )
            throws IOException
    {
        out.writeObject( key );
        out.writeObject( dataPayload );
    }

    @Override
    public void readExternal( ObjectInput in )
            throws IOException, ClassNotFoundException
    {
        this.key = (ScheduleKey)in.readObject();
        this.dataPayload = (Map)in.readObject();
    }
}
