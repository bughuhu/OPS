/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/
/* This file contains modified code derived from the excellent "Decode       *\
|* PeopleCode" open source project, maintained by Erik H                     *|
|* and available under the ISC license at                                    *|
|* http://sourceforge.net/projects/decodepcode/. The associated              *|
|* license text has been reproduced here in accordance with                  *|
|* the license requirements:                                                 *|
|* ------------------------------------------------------------------------- *|
|* Copyright (c)2011 Erik H (erikh3@users.sourceforge.net)                   *|
|*                                                                           *|
|* Permission to use, copy, modify, and/or distribute this software for any  *|
|* purpose with or without fee is hereby granted, provided that the above    *|
|* copyright notice and this permission notice appear in all copies.         *|
|*                                                                           *|
|* THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES  *|
|* WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF          *|
|* MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR   *|
|* ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES    *|
|* WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN     *|
|* ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF   *|
|* OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.            *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.bytecode;

import org.openpplsoft.pt.peoplecode.PeopleCodeByteStream;

/**
 * Assembles identifiers into their equivalent textual form.
 */
public class IdentifierAssembler extends StringAssembler {

  /**
   * Constructs a new identifier assembler.
   * @param b the starting byte of the bytecode instruction
   *   to assemble
   */
  public IdentifierAssembler(final byte b) {
    this.startByte = b;
    this.formatBitmask = AFlag.SPACE_BEFORE | AFlag.SPACE_AFTER;
  }

  @Override
  public void assemble(final PeopleCodeByteStream stream) {

    // since current byte is 0x00, we need to move the cursor
    // back first.
    stream.decrementCursor();
    stream.decrementCursor();

    stream.appendAssembledText(getString(stream));
  }
}

