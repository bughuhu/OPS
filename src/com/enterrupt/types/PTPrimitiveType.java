package com.enterrupt.types;

import com.enterrupt.runtime.*;

public abstract class PTPrimitiveType<T> extends PTType {

	protected PTPrimitiveType(Type t) {
		super(t);
	}

	public abstract T read();
	public abstract void write(T newValue);
	public abstract void systemWrite(T newValue);

	public abstract boolean equals(Object obj);
	public abstract boolean typeCheck(PTType a);

	public void copyValueFrom(PTPrimitiveType src) {
		if(src instanceof PTString) {
			((PTString)this).write(((PTString)src).read());
			return;
		}

		throw new EntVMachRuntimeException("copyValueFrom does not support " +
			"the provided source operand: " + src.toString());
	}

	public PTType dot(String s) {
		throw new EntDataTypeException("Encountered illegal dot() call on a " +
			"PTPrimitiveType where s=" + s);
	}

	protected void checkIsWriteable() {
        if(this.isSentinel()) {
            throw new EntDataTypeException("Attempted illegal write to a " +
                "sentinel PTType object.");
        }
		if(this.getFlags().contains(TFlag.READONLY)) {
			throw new EntDataTypeException("Attempted illegal write to a " +
				"readonly PTType object.");
		}
	}

	protected void checkIsSystemWriteable() {
        if(this.isSentinel()) {
            throw new EntDataTypeException("Attempted illegal system write " +
                "to a sentinel PTType object.");
        }
	}
}
