package org.commonjava.indy.metrics.zabbix.sender;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DataObject {
	long clock;
	String host;
	String key;
	String value;

	public DataObject() {

	}

	public DataObject(long clock, String host, String key, String value) {
		this.clock = clock;
		this.host = host;
		this.key = key;
		this.value = value;
	}

	static public Builder builder() {
		return new Builder();
	}

	public static class Builder {
		Long clock;
		String host;
		String key;
		String value;

		Builder() {

		}

		public Builder clock(long clock) {
			this.clock = clock;
			return this;
		}

		public Builder host(String host) {
			this.host = host;
			return this;
		}

		public Builder key(String key) {
			this.key = key;
			return this;
		}

		public Builder value(String value) {
			this.value = value;
			return this;
		}

		public DataObject build() {
			if (clock == null) {
				clock = System.currentTimeMillis() / 1000;
			}
			return new DataObject( clock, host, key, value);
		}
	}

	public long getClock() {
		return clock;
	}

	public void setClock(long clock) {
		this.clock = clock;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
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
	public static void main(String[] args)
	{
		DataObject d = new DataObject();
		d.setClock( 10 );
		d.setHost( "a" );
		d.setKey( "b" );
		d.setValue("100" );
		System.out.println( d.toString() );
	}
}
