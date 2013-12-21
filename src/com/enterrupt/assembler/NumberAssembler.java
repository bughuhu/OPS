package com.enterrupt.assembler;

import com.enterrupt.pt.peoplecode.PeopleCodeByteStream;

public class NumberAssembler extends ElementAssembler {

	byte b;
	int nBytes;

	public NumberAssembler(byte _b, int _nBytes) {
		b = _b;
		nBytes = _nBytes;
		format = AFlag.SPACE_BEFORE | AFlag.NO_SPACE_AFTER;
	}

	public byte getStartByte() {
		return b;
	}

	public void assemble(PeopleCodeByteStream stream) {

		int dValue = 0;	 // decimal position from far right going left
		String out_number = "";
		int num_bytes = nBytes - 3;

		stream.incrementCursor();	// skip first byte
		dValue = (int) stream.readNextByte();
		long val = 0, fact = 1;

		for(int i = 0; i < num_bytes; i++) {
			val += fact * (long) (stream.readNextByte() & 0xff);
			fact = fact * (long) 256;
		}

		out_number = Long.toString(val);

		if(dValue > 0 && !out_number.equals("0")) {

			while(dValue > out_number.length()) {
				out_number = "0" + out_number;
			}
			out_number = out_number.substring(0, out_number.length() - dValue) + "." +
						 out_number.substring(out_number.length() - dValue);

			if(out_number.startsWith(".")) {
				out_number = "0" + out_number;
			}
		}

		stream.appendAssembledText(out_number);
	}
}