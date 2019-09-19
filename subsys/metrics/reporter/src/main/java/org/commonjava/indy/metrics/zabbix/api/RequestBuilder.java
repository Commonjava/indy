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
package org.commonjava.indy.metrics.zabbix.api;

import java.util.concurrent.atomic.AtomicInteger;

public class RequestBuilder {
    private static final AtomicInteger nextId = new AtomicInteger(1);

	private Request request = new Request();
	
	private RequestBuilder(){

	}
	
	static public RequestBuilder newBuilder(){
		return new RequestBuilder();
	}
	
	public Request build(){
		if(request.getId() == null){
			request.setId(nextId.getAndIncrement());
		}
		return request;
	}
	
	public RequestBuilder version( String version){
		request.setJsonrpc(version);
		return this;
	}
	
	public RequestBuilder paramEntry( String key, Object value){
		request.putParam(key, value);
		return this;
	}
	
	/**
	 * Do not necessary to call this method.If don not set id, ZabbixApi will auto set request auth.. 
	 * @param auth
	 * @return
	 */
	public RequestBuilder auth( String auth){
		request.setAuth(auth);
		return this;
	}
	
	public RequestBuilder method( String method){
		request.setMethod(method);
		return this;
	}
	
	/**
	 * Do not necessary to call this method.If don not set id, RequestBuilder will auto generate.
	 * @param id
	 * @return
	 */
	public RequestBuilder id( Integer id){
		request.setId(id);
		return this;
	}
}
