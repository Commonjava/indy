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
package org.commonjava.indy.koji.model;

import io.swagger.annotations.ApiModelProperty;
import org.commonjava.indy.model.core.StoreKey;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains the result of a repair attempt. If it is a success, the error will be <b>null</b>.
 *
 * @author ruhan
 *
 */
public class KojiRepairResult
{
    @ApiModelProperty( "Original request" )
    private KojiRepairRequest request;

    @ApiModelProperty( "Error message if failed" )
    private String error;

    @ApiModelProperty( "Exception object if failed because of exception" )
    private Exception exception;

    @ApiModelProperty( "Result entries if succeeded" )
    private List<RepairResult> results;

    public KojiRepairResult() {}

    public KojiRepairResult( KojiRepairRequest request )
    {
        this.request = request;
        this.results = new ArrayList<>();
    }

    public KojiRepairResult withError( String error )
    {
        this.error = error;
        this.results.clear();
        return this;
    }

    public KojiRepairResult withError( String error, Exception e )
    {
        this.exception = e;
        return withError( error );
    }

    public KojiRepairResult withNoChange( StoreKey storeKey )
    {
        results.add( new RepairResult( storeKey ) );
        return this;
    }

    public KojiRepairResult withIgnore( StoreKey storeKey )
    {
        RepairResult result = new RepairResult( storeKey );
        result.setIgnored( true );
        results.add( result );
        return this;
    }

    public KojiRepairResult withResult( RepairResult result )
    {
        results.add( result );
        return this;
    }

    public boolean succeeded()
    {
        return error == null;
    }

    public String getError()
    {
        return error;
    }

    public KojiRepairRequest getRequest()
    {
        return request;
    }

    public List<RepairResult> getResults()
    {
        return results;
    }

    public Exception getException()
    {
        return exception;
    }

    /**
     * Repair result object of one store
     */
    public static class RepairResult
    {
        private StoreKey storeKey;

        private List<PropertyChange> changes;

        private boolean ignored;

        private Exception exception;

        public RepairResult() {}

        public RepairResult( StoreKey storeKey )
        {
            this.storeKey = storeKey;
            this.changes = new ArrayList<>();
        }

        public RepairResult( StoreKey storeKey, Exception e )
        {
            this( storeKey );
            this.exception = e;
        }

        public StoreKey getStoreKey()
        {
            return storeKey;
        }

        public List<PropertyChange> getChanges()
        {
            return changes;
        }

        public boolean isIgnored()
        {
            return ignored;
        }

        public void setIgnored( boolean ignored )
        {
            this.ignored = ignored;
        }

        public void withPropertyChange( String name, Object originalValue, Object value )
        {
            changes.add( new PropertyChange( name, originalValue, value ) );
        }

        public boolean isChanged()
        {
            return changes != null && !changes.isEmpty();
        }

        public Exception getException()
        {
            return exception;
        }

        @Override
        public String toString()
        {
            return "RepairResult{" + "storeKey=" + storeKey + ", changes=" + changes + ", ignored=" + ignored
                            + ", exception=" + exception + '}';
        }
    }

    public static class PropertyChange
    {
        private String name;

        private Object originalValue;

        private Object value;

        public PropertyChange() {}

        public PropertyChange( String name )
        {
            this.name = name;
        }

        public PropertyChange( String name, Object originalValue, Object value )
        {
            this.name = name;
            this.originalValue = originalValue;
            this.value = value;
        }

        public String getName()
        {
            return name;
        }

        public boolean isChanged()
        {
            return (originalValue == null && value != null) || !originalValue.equals( value );
        }

        public Object getOriginalValue()
        {
            return originalValue;
        }

        public void setOriginalValue( Object originalValue )
        {
            this.originalValue = originalValue;
        }

        public Object getValue()
        {
            return value;
        }

        public void setValue( Object value )
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return "PropertyChange{" + "name='" + name + '\'' + ", originalValue=" + originalValue + ", value=" + value
                            + '}';
        }
    }
}
