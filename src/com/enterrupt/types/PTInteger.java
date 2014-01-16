package com.enterrupt.types;

import java.util.EnumSet;
import com.enterrupt.runtime.*;
import org.apache.logging.log4j.*;

public class PTInteger extends PTPrimitiveType<Integer> {

	private static Logger log = LogManager.getLogger(PTInteger.class.getName());
	private static Type staticTypeFlag = Type.INTEGER;
	private Integer i;

	protected PTInteger() {
		super(staticTypeFlag);
	}

	public Integer read() {
		return this.i;
	}

    public void write(Integer newValue) {
        this.checkIsWriteable();
        this.i = newValue;
    }

    public void systemWrite(Integer newValue) {
        this.checkIsSystemWriteable();
        this.i = newValue;
    }

	public void setDefault() {
		throw new EntDataTypeException("setDefault not implemented.");
	}

	public void copyValueFrom(PTPrimitiveType src) {
		if(!(src instanceof PTInteger)) {
			throw new EntDataTypeException("Expected src to be PTInteger.");
		}
		this.write(((PTInteger)src).read());
	}

	public PTPrimitiveType add(PTPrimitiveType op) {
		if(!(op instanceof PTInteger)) {
			throw new EntDataTypeException("Expected op to be PTInteger.");
		}
		return Environment.getFromLiteralPool(
			this.read() + ((PTInteger)op).read());
	}

	public PTPrimitiveType subtract(PTPrimitiveType op) {
		if(!(op instanceof PTInteger)) {
			throw new EntDataTypeException("Expected op to be PTInteger.");
		}
		return Environment.getFromLiteralPool(
			this.read() - ((PTInteger)op).read());
	}

	public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj == null)
            return false;
        if(!(obj instanceof PTInteger))
            return false;

        PTInteger other = (PTInteger)obj;
        if(this.read().equals(other.read())) {
            return true;
        }
        return false;
    }

    public PTBoolean isEqual(PTPrimitiveType op) {
        if(!(op instanceof PTInteger)) {
            throw new EntDataTypeException("Expected op to be PTInteger.");
        }
		if(this.i.compareTo(((PTInteger)op).read()) == 0) {
			return Environment.TRUE;
		}
		return Environment.FALSE;
    }

    public PTBoolean isGreaterThan(PTPrimitiveType op) {
        if(!(op instanceof PTInteger)) {
            throw new EntDataTypeException("Expected op to be PTInteger.");
        }
        if(this.i.compareTo(((PTInteger)op).read()) > 0) {
            return Environment.TRUE;
        }
        return Environment.FALSE;
    }

    public PTBoolean isGreaterThanOrEqual(PTPrimitiveType op) {
		throw new EntDataTypeException("isGreaterThanOrEqual not "
			+ "supported.");
	}

    public PTBoolean isLessThan(PTPrimitiveType op) {
        if(!(op instanceof PTInteger)) {
            throw new EntDataTypeException("Expected op to be PTInteger.");
        }
        if(this.i.compareTo(((PTInteger)op).read()) < 0) {
            return Environment.TRUE;
        }
        return Environment.FALSE;
	}

    public PTBoolean isLessThanOrEqual(PTPrimitiveType op) {
        if(!(op instanceof PTInteger)) {
            throw new EntDataTypeException("Expected op to be PTInteger.");
        }
        if(this.i.compareTo(((PTInteger)op).read()) <= 0) {
            return Environment.TRUE;
        }
        return Environment.FALSE;
	}

	public boolean typeCheck(PTType a) {
		return (a instanceof PTInteger &&
			this.getType() == a.getType());
	}

    public static PTInteger getSentinel() {

        // If the sentinel has already been cached, return it immediately.
        String cacheKey = getCacheKey();
        if(PTType.isSentinelCached(cacheKey)) {
            return (PTInteger)PTType.getCachedSentinel(cacheKey);
        }

        // Otherwise, create a new sentinel type and cache it before returning it.
        PTInteger sentinelObj = new PTInteger();
        PTType.cacheSentinel(sentinelObj, cacheKey);
        return sentinelObj;
    }

    public PTPrimitiveType alloc() {
        PTInteger newObj = new PTInteger();
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
		b.append(",i=").append(this.i);
		return b.toString();
	}
}
