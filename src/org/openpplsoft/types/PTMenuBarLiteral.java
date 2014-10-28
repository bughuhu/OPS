/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.types;

import java.util.List;

import org.openpplsoft.pt.*;
import org.openpplsoft.runtime.*;

/**
 * Represents a PeopleTools menu bar literal.
 */
public final class PTMenuBarLiteral extends PTString {

  public PTMenuBarLiteral(final PTTypeConstraint origTc) {
    super(origTc);
  }

  public PTMenuBarLiteral(final String bStr) {
    super(new PTTypeConstraint<PTMenuBarLiteral>(PTMenuBarLiteral.class));

    if(!bStr.toLowerCase().startsWith("barname.")) {
      throw new OPSVMachRuntimeException("Expected bStr to start "
          + "with 'BarName.' (case-insensitive) while alloc'ing "
          + "PTMenuBarLiteral; bStr = " + bStr);
    }
    this.write(bStr.substring(bStr.indexOf(".") + 1));
  }
}
