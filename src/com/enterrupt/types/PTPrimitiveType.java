package com.enterrupt.types;

public abstract class PTPrimitiveType<T> extends PTType {

	protected PTPrimitiveType(Type t) {
		super(t);
	}

	public abstract T read();
	public abstract void write(T newValue);
	public abstract void systemWrite(T newValue);

	public abstract PTPrimitiveType alloc();
	public abstract boolean equals(Object obj);
	public abstract boolean typeCheck(PTType a);

	public abstract PTBoolean isEqual(PTPrimitiveType op);
	public abstract PTBoolean isGreaterThan(PTPrimitiveType op);

	public void copyValueFrom(PTPrimitiveType src) {

		if(src instanceof PTString) {
			((PTString)this).write(((PTString)src).read());

		} else if(src instanceof PTInteger) {
			((PTInteger)this).write(((PTInteger)src).read());

		} else {
			throw new EntDataTypeException("copyValueFrom does not support " +
				"the provided source operand: " + src.toString());
		}
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
