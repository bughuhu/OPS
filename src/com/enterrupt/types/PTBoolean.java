package com.enterrupt.types;

import java.util.EnumSet;

public class PTBoolean extends PTPrimitiveType<Boolean> {

	private Boolean b;

	protected PTBoolean() {
		super(Type.BOOLEAN);
	}

	public Boolean read() {
		return this.b;
	}

    public void write(Boolean newValue) {
        this.checkIsWriteable();
        this.b = newValue;
    }

    public void systemWrite(Boolean newValue) {
        this.checkIsSystemWriteable();
        this.b = newValue;
    }

    public boolean equals(Object obj) {
        if(obj == this)
            return true;
        if(obj == null)
            return false;
        if(!(obj instanceof PTBoolean))
            return false;

        PTBoolean other = (PTBoolean)obj;
		if(this.read().equals(other.read())) {
            return true;
        }
        return false;
    }

	public boolean typeCheck(PTType a) {
		return (a instanceof PTBoolean &&
			this.getType() == a.getType());
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder(super.toString());
		b.append(",b=").append(this.b);
		return b.toString();
	}
}

