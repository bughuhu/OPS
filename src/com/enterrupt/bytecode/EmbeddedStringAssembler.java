package com.enterrupt.bytecode;

/**
 * ============================================================
 * This file contains source code derived from the excellent
 * Decode PeopleCode open source project, maintained by Erik H
 * and available under the ISC license at
 * http://sourceforge.net/projects/decodepcode/. The associated
 * license text has been reproduced here in accordance with
 * the license requirements.
 * ============================================================
 * Copyright (c)2011 Erik H (erikh3@users.sourceforge.net)
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 * ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 * OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

import com.enterrupt.pt.peoplecode.PeopleCodeByteStream;

public class EmbeddedStringAssembler extends PureStringAssembler {

	String pre, post;

	public EmbeddedStringAssembler(byte _b, String _pre, String _post, int _format) {
		super(_b);
		this.pre = _pre;
		this.post = _post;
		this.format = _format;
	}

	public EmbeddedStringAssembler(byte _b, String _pre, String _post) {
		this(_b, _pre, _post, AFlag.SPACE_BEFORE_AND_AFTER);
	}

	public void assemble(PeopleCodeByteStream stream) {
		stream.appendAssembledText(this.pre
			+ getString(stream).replace("\"", "\"\"") + this.post);
	}
}
