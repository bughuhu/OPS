package com.enterrupt.interpreter;

import java.lang.reflect.*;

public class FunctionToken extends Token {

	public Method fnTarget;

	public FunctionToken(int type) {
		super(type);
	}

	public void invoke() throws Exception {
        this.fnTarget.invoke(RunTimeEnvironment.class);
    }

	public boolean isNull() {
		return false;
	}
}
