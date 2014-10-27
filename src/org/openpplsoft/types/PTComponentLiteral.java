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
 * Represents a PeopleTools component literal.
 */
public final class PTComponentLiteral extends PTString {

  public PTComponentLiteral(final PTTypeConstraint origTc) {
    super(origTc);
  }

  public PTComponentLiteral(final String cStr) {
    super(new PTTypeConstraint<PTComponentLiteral>(PTComponentLiteral.class));

    if(!cStr.toLowerCase().startsWith("component.")) {
      throw new OPSVMachRuntimeException("Expected cStr to start "
          + "with 'Component.' (case-insensitive) while creating "
          + "PTComponentLiteral; cStr = "
          + cStr);
    }

    this.write(cStr.substring(cStr.indexOf(".") + 1));
    this.setReadOnly();
  }

  /**
   * Returns the name of the component represented by this literal.
   * @return the name of the component
   */
  public String getComponentName() {
    return this.read();
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder(super.toString());
    b.append(",literal=").append(this.read());
    return b.toString();
  }
}
