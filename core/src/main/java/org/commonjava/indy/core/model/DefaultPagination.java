/**
 * Copyright (C) 2011-2017 Red Hat, Inc. (https://github.com/Commonjava/indy)
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
package org.commonjava.indy.core.model;

import org.commonjava.indy.core.ctl.PaginationHandler;

/**
 * Created by yma on 2018/1/15.
 */
public class DefaultPagination<T>
        implements Pagination<T>
{

    private GenericPagination pagination;

    private PaginationHandler<T> handler;

    private T currData;

    public DefaultPagination( GenericPagination pagination, PaginationHandler<T> handler )
    {
        this.pagination = pagination;
        this.handler = handler;
        setCurrData();
    }

    public DefaultPagination()
    {
    }

    @Override
    public GenericPagination getPagination()
    {
        return pagination;
    }

    public void setPagination( GenericPagination pagination )
    {
        this.pagination = pagination;
    }

    @Override
    public T getCurrData()
    {
        return currData;
    }

    public void setCurrData()
    {
        this.currData = handler.getCurrData( pagination );
    }
}
