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
package org.commonjava.indy.ftest.metrics.client;

import org.commonjava.indy.client.core.IndyClientException;
import org.commonjava.indy.client.core.IndyClientModule;
import org.commonjava.indy.client.core.util.UrlUtils;

/**
 * Created by xiabai on 3/22/17.
 */
public class IndyMetricsFtestClientModule
                extends IndyClientModule
{
    private static String TIMTER_PATH = "/ftest/metrics/timer";
    private static String METER_PATH = "/ftest/metrics/meter";
    private static String TIMER_COUNT_PATH = "/ftest/metrics/metricRegistry/timer";
    private static String TIMER_COUNT_EXCEPTION_PATH = "/ftest/metrics/metricRegistry/timer/exception";
    private static String MERTER_COUNT_PATH = "/ftest/metrics/metricRegistry/meter";
    private static String MERTER_COUNTEXCEPTION__PATH = "/ftest/metrics/metricRegistry/meter/exception";

    public String getTimerWithOutException() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", TIMTER_PATH+"/false" ), String.class );
    }

    public String getTimerWithException() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", TIMTER_PATH+"/true" ), String.class );
    }

    public String getMeterWithOutException() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", METER_PATH+"/false" ), String.class );
    }

    public String getMeterWithException() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", METER_PATH+"/true" ), String.class );
    }

    public String getTimerCount()throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", TIMER_COUNT_PATH), String.class );
    }

    public String getTimerCountWithException() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", TIMER_COUNT_EXCEPTION_PATH ), String.class );
    }

    public String getMeterCount() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", MERTER_COUNT_PATH ), String.class );
    }

    public String getMeterCountWithException() throws IndyClientException
    {
        return http.get( UrlUtils.buildUrl( "", MERTER_COUNTEXCEPTION__PATH ), String.class );
    }

}
