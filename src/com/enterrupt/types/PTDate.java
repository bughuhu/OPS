package com.enterrupt.types;

import java.util.*;
import java.text.SimpleDateFormat;

public class PTDate extends PTPrimitiveType<String> {

	private static Type staticTypeFlag = Type.DATE;
	private String d;

	protected PTDate() {
		super(staticTypeFlag);

		// default value is current date unless date is overridden for trace file
		// verification purposes.
		if(System.getProperty("traceFileDate") != null) {
			this.d = System.getProperty("traceFileDate");
		} else {
			d = new SimpleDateFormat("yyyy-MM-dd")
						.format(Calendar.getInstance().getTime());
		}
	}

	public String read() {
		return this.d;
	}

    public void write(String newValue) {
        this.checkIsWriteable();
        this.d = newValue;
    }

    public void systemWrite(String newValue) {
        this.checkIsSystemWriteable();
        this.d = newValue;
    }

	public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj == null)
            return false;
        if(!(obj instanceof PTDate))
            return false;

        PTDate other = (PTDate)obj;
        if(this.read().equals(other.read())) {
            return true;
        }
        return false;
    }

	public boolean typeCheck(PTType a) {
		return (a instanceof PTDate &&
			this.getType() == a.getType());
	}

    public static PTDate getSentinel() {

        // If the sentinel has already been cached, return it immediately.
        String cacheKey = getCacheKey();
        if(PTType.isSentinelCached(cacheKey)) {
            return (PTDate)PTType.getCachedSentinel(cacheKey);
        }

        // Otherwise, create a new sentinel type and cache it before returning it.
        PTDate sentinelObj = new PTDate();
        PTType.cacheSentinel(sentinelObj, cacheKey);
        return sentinelObj;
    }

    public PTPrimitiveType alloc() {
        PTDate newObj = new PTDate();
        PTType.clone(this, newObj);
        return newObj;
    }

    private static String getCacheKey() {
        StringBuilder b = new StringBuilder(staticTypeFlag.name());
        return b.toString();
    }

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(super.toString());
		b.append(",d=").append(this.d);
		return b.toString();
	}
}