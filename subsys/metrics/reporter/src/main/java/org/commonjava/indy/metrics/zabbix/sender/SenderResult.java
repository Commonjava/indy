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
package org.commonjava.indy.metrics.zabbix.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 
 */
public class SenderResult {
	int processed;
	int failed;
	int total;

	float spentSeconds;

	/**
	 * sometimes zabbix server will return "[]".
	 */
	boolean bReturnEmptyArray = false;

	/**
	 * if all sended data are processed, will return true, else return false.
	 * 
	 * @return
	 */
	public boolean success() {
		return !bReturnEmptyArray && processed == total;
	}

	public int getProcessed() {
		return processed;
	}

	public void setProcessed(int processed) {
		this.processed = processed;
	}

	public int getFailed() {
		return failed;
	}

	public void setFailed(int failed) {
		this.failed = failed;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public float getSpentSeconds() {
		return spentSeconds;
	}

	public void setSpentSeconds(float spentSeconds) {
		this.spentSeconds = spentSeconds;
	}

	public void setbReturnEmptyArray(boolean bReturnEmptyArray) {
		this.bReturnEmptyArray = bReturnEmptyArray;
	}

	@Override
	public String toString() {
		ObjectMapper mapper = new ObjectMapper();

		try
		{
			return mapper.writeValueAsString( this );
		}
		catch ( JsonProcessingException e )
		{
			e.printStackTrace();
			throw new RuntimeException( e );
		}
	}
}
