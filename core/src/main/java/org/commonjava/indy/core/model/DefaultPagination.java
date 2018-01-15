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

import java.util.List;

/**
 * Created by yma on 2018/1/15.
 */
public class DefaultPagination<T>
        implements Pagination<T>
{

    protected int pageIndex;

    protected int pageSize;

    protected PaginationHandler<T> handler;

    protected T currData;

    protected List<T> currDataList;

    public DefaultPagination( int pageIndex, int pageSize, PaginationHandler<T> handler )
    {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.handler = handler;
        setCurrData();
        setCurrDataList();
    }

    public DefaultPagination()
    {
    }

    @Override
    public int getPageIndex()
    {
        return pageIndex;
    }

    public void setPageIndex( int pageIndex )
    {
        this.pageIndex = pageIndex;
    }

    @Override
    public int getPageSize()
    {
        return pageSize;
    }

    public void setPageSize( int pageSize )
    {
        this.pageSize = pageSize;
    }

    @Override
    public T getCurrData()
    {
        return currData;
    }

    public void setCurrData()
    {
        this.currData = handler.getCurrData( pageIndex, pageSize );
    }

    @Override
    public List<T> getCurrDataList()
    {
        return currDataList;
    }

    public void setCurrDataList()
    {
        this.currDataList = handler.getCurrDataList( pageIndex, pageSize );
    }
}
