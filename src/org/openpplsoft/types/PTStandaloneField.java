/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.types;

import java.lang.reflect.Method;
import java.util.*;
import java.sql.*;

import org.openpplsoft.runtime.*;
import org.openpplsoft.buffers.*;
import org.openpplsoft.pt.*;
import org.openpplsoft.pt.pages.*;
import org.openpplsoft.sql.*;
import org.openpplsoft.trace.*;
import org.openpplsoft.pt.peoplecode.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class PTStandaloneField extends PTField {

  private static Logger log = LogManager.getLogger(PTStandaloneField.class.getName());

  private static Map<String, Method> ptMethodTable;

  private PTRecord parentRecord;
  private RecordField recFieldDefn;
  private RecordFieldBuffer recFieldBuffer;
  private PTImmutableReference<PTPrimitiveType> valueRef;
  private PTImmutableReference<PTBoolean> visiblePropertyRef,
      displayOnlyPropertyRef;
  private PTImmutableReference<PTString> fldNamePropertyRef;
  private boolean isGrayedOut;

  static {
    final String PT_METHOD_PREFIX = "PT_";

    // cache pointers to PeopleTools Field methods.
    final Method[] methods = PTStandaloneField.class.getMethods();
    ptMethodTable = new HashMap<String, Method>();
    for (Method m : methods) {
      if (m.getName().indexOf(PT_METHOD_PREFIX) == 0) {
        ptMethodTable.put(m.getName().substring(
            PT_METHOD_PREFIX.length()), m);
      }
    }
  }

  public PTStandaloneField(final PTFieldTypeConstraint origTc, final PTRecord pRecord,
      final RecordField rfd) {
    super(origTc);
    this.parentRecord = pRecord;
    this.recFieldDefn = rfd;
    this.init();
  }

  public PTStandaloneField(final PTFieldTypeConstraint origTc, final PTRecord pRecord,
      final RecordFieldBuffer recFldBuffer) {
    super(origTc);
    this.parentRecord = pRecord;
    this.recFieldDefn = recFldBuffer.getRecFldDefn();
    this.recFieldBuffer = recFldBuffer;
    this.init();
  }

  private void init() {
    final PTTypeConstraint valueTc
        = recFieldDefn.getTypeConstraintForUnderlyingValue();

    try {
      /*
       * Initialize read/write properties.
       */
      this.valueRef
          = new PTImmutableReference<PTPrimitiveType>(valueTc,
              (PTPrimitiveType) valueTc.alloc());
      this.visiblePropertyRef
          = new PTImmutableReference<PTBoolean>(
              PTBoolean.getTc(), new PTBoolean(true));
      // NOTE: Technically this is supposed to be defaulted to the value
      // specified in App Designer. Checking if the first bit of FIELDUSE in
      // PgToken is set to 1 will tell you if the field is display only. However,
      // at this time, I am not linking pages to fields. If you have issues with
      // logic related to DisplayOnly, you may need to associate fields with pages.
      this.displayOnlyPropertyRef
          = new PTImmutableReference<PTBoolean>(
              PTBoolean.getTc(), PTBoolean.getTc().alloc());

      /*
       * Initialize read-only properties.
       */
      this.fldNamePropertyRef
          = new PTImmutableReference<PTString>(
              PTString.getTc(), new PTString(this.recFieldDefn.FIELDNAME));
      this.fldNamePropertyRef.deref().setReadOnly();

    } catch (final OPSTypeCheckException opstce) {
      throw new OPSVMachRuntimeException(opstce.getMessage(), opstce);
    }
  }

  /**
   * MQUINN 12-02-2014 : TO REMOVE after split:
   */
  public void runNonConstantFieldDefaultProcessing(final FieldDefaultProcSummary summary) {}
  public void runConstantFieldDefaultProcessing(
      final FieldDefaultProcSummary fldDefProcSummary) {}
  public PTRecord resolveContextualCBufferRecordReference(final String recName) {
    return null;
  }
  public PTReference<PTField> resolveContextualCBufferRecordFieldReference(
      final String recName, final String fieldName) {
    return null;
  }
  public PTRowset resolveContextualCBufferScrollReference(
      final PTScrollLiteral scrollName) {
    return null;
  }
  public void generateKeylist(
      final String fieldName, final Keylist keylist) {}
  public void generateKeylist(final Keylist keylist) {}
  public int getIndexPositionOfThisFieldInParentRecord() {
    return -5;
  }
  public void emitScrolls(final String indent) {}
  public void fireEvent(final PCEvent event,
      final FireEventSummary fireEventSummary) {}
  public PTRecord getParentRecord() {
    return this.parentRecord;
  }
  public void runFieldDefaultProcessing(
      final FieldDefaultProcSummary fldDefProcSummary) {}

  public PTPrimitiveType getValue() {
    return this.valueRef.deref();
  }

  public RecordField getRecordFieldDefn() {
    return this.recFieldDefn;
  }

  public PTImmutableReference dotProperty(String s) {
    if(s.toLowerCase().equals("value")) {

      /*
       * Why is this check here and not in Record's PT_GetField, you may ask?
       * Furthermore, why are we only throwing an exception when attempting to
       * write to the Value of a field that is not in the component buffer (and not
       * Visible, DisplayOnly, etc.)?
       * Because apparently that's what PS does according to the tracefile.
       * This exception will be converted to a PCE in visitStmtAssign of the
       * interpreter.
       */
      if (this.parentRecord != null
          && this.parentRecord.getRecBuffer() != null
          && this.recFieldBuffer == null) {
        throw new OPSIllegalNonCBufferFieldAccessAttempt(
            this.recFieldDefn.RECNAME, this.recFieldDefn.FIELDNAME);
      }
      return this.valueRef;
    } else if(s.toLowerCase().equals("visible")) {
      return this.visiblePropertyRef;
    } else if(s.toLowerCase().equals("name")) {
      return this.fldNamePropertyRef;
    } else if(s.toLowerCase().equals("displayonly")) {
      return this.displayOnlyPropertyRef;
    }
    return null;
  }

  public int determineRowIndex() {
    if (this.parentRecord != null
        && this.parentRecord.getParentRow() != null) {
      return this.parentRecord.getParentRow().getIndexOfThisRowInParentRowset();
    }

    throw new OPSVMachRuntimeException("Failed to determine ancestral row "
        + "index; path to parent record and/or row contains null somewhere.");
  }

  public void setBlank() {
    valueRef.deref().setBlank();
  }

  public int determineScrollLevel() {
    if (this.parentRecord != null) {
      return this.parentRecord.determineScrollLevel();
    }

    throw new OPSVMachRuntimeException("Failed to determine scroll level for "
        + "this field; parent record is null.");
  }

  @Override
  public Callable dotMethod(final String s) {
    if (ptMethodTable.containsKey(s)) {
      return new Callable(ptMethodTable.get(s), this);
    }
    return null;
  }

  /**
   * From PeopleBooks
   * (http://docs.oracle.com/cd/E38689_01/pt853pbr0/eng/pt/tpcl/langref_PeopleCodeBuilt-inFunctionsandLanguageConstructs-073e6a.html#SetDefault-073c69)
   * :
   * "Use the SetDefault function to set a field to a null value,
   * so that the next time default processing occurs, it is set
   * to its default value"
   * TODO(mquinn): THERE ARE CASES WHERE CALLING THIS METHOD WILL ABORT
   * CONTINUED EXECUTION OF THE CALLING FUNCTION / PROGRAM; you must
   * implement these cases in the future.
   */
  public void PT_SetDefault() {
    final List<PTType> args = Environment.getArgsFromCallStack();
    if (args.size() != 0) {
      throw new OPSVMachRuntimeException("Expected no args to SetDefault.");
    }
    this.setBlank();
  }

  public void PT_GetLongLabel() {
    final List<PTType> args = Environment.getArgsFromCallStack();
    if (args.size() != 1
        || !(args.get(0) instanceof PTString)) {
      throw new OPSVMachRuntimeException("Expected single string arg to GetLongLabel.");
    }

    final String labelId = ((PTString) args.get(0)).read();
    final FieldLabel label = this.recFieldDefn.getLabelById(labelId);

    // If label does not exist, PT documentation indicates that a "Null"
    // (blank) string should be returned.
    if (label == null) {
      Environment.pushToCallStack(new PTString(""));
    } else {
      Environment.pushToCallStack(new PTString(label.getLongName()));
    }
  }

  /**
   * Calls to make a field read-only should make the
   * field's value read-only as well.
   */
  @Override
  public void setReadOnly() {
    super.setReadOnly();
    if(this.valueRef != null) {
      this.valueRef.setReadOnly();
    }
  }

  public RecordFieldBuffer getRecordFieldBuffer() {
    return this.recFieldBuffer;
  }

  public void grayOut() {
    this.isGrayedOut = true;
  }

  public void hide() {
    this.visiblePropertyRef.deref().write(false);
  }

  public void unhide() {
    this.visiblePropertyRef.deref().write(true);
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder(super.toString());
    b.append(":").append(recFieldDefn.FIELDNAME);
    b.append(",valueRef=").append(valueRef.toString());
    return b.toString();
  }
}
