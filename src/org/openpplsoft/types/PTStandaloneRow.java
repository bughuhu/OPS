/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.types;

import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.openpplsoft.buffers.*;
import org.openpplsoft.pt.*;
import org.openpplsoft.runtime.*;
import org.openpplsoft.trace.*;

/**
 * Represents a PeopleTools row definition; contains
 * 1 to n child records and 0 to m child rowsets.
 */
public final class PTStandaloneRow extends PTRow<PTStandaloneRowset, PTStandaloneRecord> {

  private static Map<String, Method> ptMethodTable;

  private static final Logger log = LogManager.getLogger(PTStandaloneRow.class.getName());

  static {
    final String PT_METHOD_PREFIX = "PT_";
    // cache pointers to PeopleTools Row methods.
    final Method[] methods = PTStandaloneRow.class.getMethods();
    ptMethodTable = new HashMap<String, Method>();
    for (Method m : methods) {
      if (m.getName().indexOf(PT_METHOD_PREFIX) == 0) {
        ptMethodTable.put(m.getName().substring(
            PT_METHOD_PREFIX.length()), m);
      }
    }
  }

  public PTStandaloneRow(final PTRowTypeConstraint origTc,
      final PTStandaloneRowset pRowset,
      final Set<Record> recDefnsToRegister,
      final Map<String, ScrollBuffer> childScrollDefnsToRegister) {
    super(origTc);
    this.parentRowset = pRowset;

    // Register all record defns in the provided set.
    for(final Record recDefn : recDefnsToRegister) {
      this.registerRecordDefn(recDefn);
    }

    // Register all child scroll defns in the provided map.
    for(Map.Entry<String, ScrollBuffer> entry
        : childScrollDefnsToRegister.entrySet()) {
      this.registerChildScrollDefn(entry.getValue());
    }

    try {
      /*
       * Initialize read/write properties.
       */
      this.selectedPropertyRef
          = new PTImmutableReference<PTBoolean>(
              PTBoolean.getTc(), new PTBoolean(false));
    } catch (final OPSTypeCheckException opstce) {
      throw new OPSVMachRuntimeException(opstce.getMessage(), opstce);
    }
  }

  // Used to register record defns that don't have an associated
  // buffer (i.e., standalone rows/rowsets)
  public void registerRecordDefn(final Record recDefn) {
    // Only register the record defn if it hasn't already been registered.
    if (!this.registeredRecordDefns.contains(recDefn)) {
      this.registeredRecordDefns.add(recDefn);
      this.recordMap.put(recDefn.RECNAME,
          new PTRecordTypeConstraint().allocStandaloneRecord(this, recDefn));
    }
  }

  /**
   * MQUINN 12-03-2014 : Remove after split.
   */
  public void registerRecordDefn(final RecordBuffer recBuffer) {
  }
  public void registerChildScrollDefn(final ScrollBuffer childScrollDefn) {
  }
  public void emitScrolls(final String indent) {
  }
  public void fireEvent(final PCEvent event,
      final FireEventSummary fireEventSummary) {
  }
  public void runFieldDefaultProcessing(
      final FieldDefaultProcSummary fldDefProcSummary) {
  }
  public PTRecord resolveContextualCBufferRecordReference(final String recName) {
    return null;
  }
  public PTReference<PTField> resolveContextualCBufferRecordFieldReference(
      String recName, String fldName) {
    return null;
  }
  public PTRowset resolveContextualCBufferScrollReference(
      final PTScrollLiteral scrollName) {
    return null;
  }
  public void generateKeylist(
      final String fieldName, final Keylist keylist) {
  }
  public int getIndexPositionOfRecord(final PTRecord rec) {
    return -5;
  }
  public int getIndexOfThisRowInParentRowset() {
    return -5;
  }
  public int determineScrollLevel() {
    return -5;
  }

  @Override
  public String toString() {
    return new StringBuilder(super.toString())
      .append(",childRecordRecDefns=").append(this.recordMap.keySet())
      .append(",childRowsetRecDefns=").append(this.rowsetMap.keySet())
      .toString();
  }
}
