/*******************************************************************************
 * Copyright 2011 John Casey
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.commonjava.aprox.couch.data;

import org.commonjava.aprox.core.conf.ProxyConfiguration;
import org.commonjava.aprox.couch.data.ProxyAppDescription.View;
import org.commonjava.couch.db.model.ViewRequest;

public class ProxyViewRequest
    extends ViewRequest
{

    public ProxyViewRequest( final ProxyConfiguration config, final View view )
    {
        super( ProxyAppDescription.APP_NAME, view.viewName() );
        setParameter( INCLUDE_DOCS, true );
    }

    public ProxyViewRequest( final ProxyConfiguration config, final View view, final String key )
    {
        this( config, view );
        setParameter( KEY, key );
    }

}
