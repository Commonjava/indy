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

/**
 * Created by yma on 2018/1/17.
 */
public abstract class GenericPagination
{
    protected int pageIndex;

    protected int pageSize;

    public GenericPagination( int pageIndex, int pageSize )
    {
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
    }

    public GenericPagination()
    {
    }

    public int getPageIndex()
    {
        return pageIndex;
    }

    public void setPageIndex( int pageIndex )
    {
        this.pageIndex = pageIndex;
    }

    public int getPageSize()
    {
        return pageSize;
    }

    public void setPageSize( int pageSize )
    {
        this.pageSize = pageSize;
    }

    public boolean allowPaging()
    {
        return ( pageIndex >= 0 ) ? true : false;
    }
}
