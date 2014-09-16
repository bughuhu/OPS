/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.types;

import java.util.EnumSet;

import org.apache.logging.log4j.*;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import org.openpplsoft.runtime.*;

/**
 * Implementation of the PeopleTools char data type.
 */
public final class PTChar extends PTPrimitiveType<Character> {

  private static Logger log = LogManager.getLogger(PTChar.class.getName());
  private static PTTypeConstraint<PTChar> charTc;

  private Character c;

  static {
    charTc = new PTTypeConstraint<PTChar>(PTChar.class);
  }

  public PTChar(PTTypeConstraint origTc) {
    super(origTc);
  }

  public static PTTypeConstraint<PTChar> getTc() {
    return charTc;
  }

  @Override
  public Character read() {
    return this.c;
  }

  @Override
  public String readAsString() {
    return Character.toString(this.c);
  }

  @Override
  public void write(final Character newValue) {
    this.checkIsWriteable();
    this.c = newValue;
  }

  public void write(final String newValue) {
    this.checkIsWriteable();
    log.debug("Writing to PTChar; raw string is: {}", newValue);
    if(newValue.length() > 1) {
      throw new OPSVMachRuntimeException("Illegal attempt to write string ("
          + newValue + ") to PTChar.");
    }
    if(newValue.length() == 0) {
      throw new OPSVMachRuntimeException("Illegal attempt to write empty "
          + "string to PTChar.");
    }
    this.c = newValue.charAt(0);
  }

  @Override
  public void systemWrite(final Character newValue) {
    this.c = newValue;
  }

  @Override
  public void setDefault() {
    this.c = ' ';
  }

  @Override
  public void copyValueFrom(final PTPrimitiveType src) {
    if (!(src instanceof PTChar)) {
      throw new OPSDataTypeException("Expected src to be PTChar.");
    }
    this.write(((PTChar) src).read());
  }

  @Override
  public PTBoolean isEqual(final PTPrimitiveType op) {
    if (op instanceof PTChar && this.c.equals(((PTChar) op).read())) {
      return new PTBoolean(true);

    } else if(op instanceof PTString) {
        /*
         * If op is a string of length 1, comparison can continue.
         */
        PTString str = (PTString) op;
        if(str.read().length() == 1
            && this.c.equals(str.read().charAt(0))) {
          return new PTBoolean(true);
        }

    } else if(op instanceof PTInteger) {
        /*
         * If op is an integer, comparison can continue.
         */
        if(Character.getNumericValue(this.c)
            == ((Integer) op.read()).intValue()) {
          return new PTBoolean(true);
        }

    } else {
      throw new OPSDataTypeException("Expected op to be PTChar; is: " + op);
    }
    return new PTBoolean(false);
  }

  @Override
  public PTBoolean isGreaterThan(final PTPrimitiveType op) {
    if (!(op instanceof PTChar)) {
      throw new OPSDataTypeException("Expected op to be PTChar.");
    }
    if (this.c.compareTo(((PTChar) op).read()) > 0) {
      return new PTBoolean(true);
    }
    return new PTBoolean(false);
  }

  @Override
  public PTBoolean isGreaterThanOrEqual(final PTPrimitiveType op) {
    throw new OPSDataTypeException("isGreaterThanOrEqual not "
        + "supported.");
  }

  @Override
  public PTBoolean isLessThan(final PTPrimitiveType op) {
    if (!(op instanceof PTChar)) {
      throw new OPSDataTypeException("Expected op to be PTChar.");
    }
    if (this.c.compareTo(((PTChar) op).read()) < 0) {
      return new PTBoolean(true);
    }
    return new PTBoolean(false);
  }

  @Override
  public PTBoolean isLessThanOrEqual(final PTPrimitiveType op) {
    throw new OPSDataTypeException("isLessThanOrEqual not supported "
        + "for PTChar.");
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj == this) {
      return true;
    } else if (obj == null) {
      return false;
    } else if (!(obj instanceof PTChar)) {
      return false;
    }

    final PTChar other = (PTChar) obj;
    if (this.read().equals(other.read())) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int HCB_INITIAL = 31, HCB_MULTIPLIER = 419;

    return new HashCodeBuilder(HCB_INITIAL,
        HCB_MULTIPLIER).append(this.read()).toHashCode();
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder(super.toString());
    b.append(",c=").append(this.c.toString());
    return b.toString();
  }
}
