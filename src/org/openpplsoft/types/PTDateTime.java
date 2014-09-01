/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.types;

import java.text.SimpleDateFormat;

import java.util.*;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import org.openpplsoft.runtime.*;

public final class PTDateTime extends PTPrimitiveType<String> {

  private String d;

  public PTDateTime(final PTTypeConstraint origTc) {
    super(origTc);

    // default value is current date and time.
    d = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        .format(Calendar.getInstance().getTime());
  }

  public String read() {
    return this.d;
  }

  public String readAsString() {
    return this.d;
  }

  public void write(String newValue) {
    this.checkIsWriteable();
    this.d = newValue;
  }

  public void systemWrite(String newValue) {
    this.d = newValue;
  }

  public void copyValueFrom(PTPrimitiveType src) {
    throw new OPSDataTypeException("copyValueFrom is not yet supported.");
  }

  public PTPrimitiveType add(PTPrimitiveType op) {
    throw new OPSDataTypeException("add() not supported.");
  }

  public PTPrimitiveType subtract(PTPrimitiveType op) {
    throw new OPSVMachRuntimeException("subtract() not supported.");
  }

  @Override
  public PTPrimitiveType mul(PTPrimitiveType op) {
    throw new OPSVMachRuntimeException("mul() not supported.");
  }

  @Override
  public PTPrimitiveType div(PTPrimitiveType op) {
    throw new OPSVMachRuntimeException("div() not supported.");
  }

  public void setDefault() {
    this.d = null;
  }

  public PTBoolean isEqual(PTPrimitiveType op) {
    throw new OPSDataTypeException("isEqual not implemented for " +
      "Date.");
  }

  public PTBoolean isGreaterThan(PTPrimitiveType op) {
    throw new OPSDataTypeException("isGreaterThan not implemented for " +
      "Date.");
  }

  public PTBoolean isGreaterThanOrEqual(PTPrimitiveType op) {
    if(!(op instanceof PTDateTime)) {
      throw new OPSDataTypeException("Expected op to be PTDateTime.");
    }
    if(this.d.compareTo(((PTDateTime)op).read()) >= 0) {
      return Environment.TRUE;
    }
    return Environment.FALSE;
  }

  public PTBoolean isLessThan(PTPrimitiveType op) {
    throw new OPSDataTypeException("isLessThan is not supported for " +
      "Date.");
  }

  public PTBoolean isLessThanOrEqual(PTPrimitiveType op) {
    if(!(op instanceof PTDateTime)) {
      throw new OPSDataTypeException("Expected op to be PTDateTime.");
    }
    if(this.d.compareTo(((PTDateTime)op).read()) <= 0) {
      return Environment.TRUE;
    }
    return Environment.FALSE;
  }

  public boolean equals(Object obj) {
    if(obj == this)
      return true;
    if(obj == null)
      return false;
    if(!(obj instanceof PTDateTime))
      return false;

    PTDateTime other = (PTDateTime)obj;
    if(this.read().equals(other.read())) {
      return true;
    }
    return false;
  }

  @Override
  public int hashCode() {
    final int HBC_INITIAL = 991, HBC_MULTIPLIER = 359;

    return new HashCodeBuilder(HBC_INITIAL,
        HBC_MULTIPLIER).append(this.read()).toHashCode();
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder(super.toString());
    b.append(",d=").append(this.d);
    return b.toString();
  }
}
