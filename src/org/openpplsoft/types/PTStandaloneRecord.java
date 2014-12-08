/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.types;

import java.lang.reflect.Method;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.openpplsoft.pt.*;
import org.openpplsoft.pt.pages.*;
import org.openpplsoft.runtime.*;
import org.openpplsoft.trace.*;
import org.openpplsoft.sql.*;
import org.openpplsoft.pt.peoplecode.*;
import org.openpplsoft.buffers.*;

/**
 * Represents a PeopleTools standalone record (not in the component buffer).
 */
public final class PTStandaloneRecord extends PTRecord<PTStandaloneRow,
    PTStandaloneField> {

  private static Logger log = LogManager.getLogger(PTStandaloneRecord.class.getName());

  private static Map<String, Method> ptMethodTable;

  /**
   * MQUINN 12-03-2014: TODO: Remove after split.
   */
  private RecordBuffer recBuffer;

  static {
    final String PT_METHOD_PREFIX = "PT_";

    // cache pointers to PeopleTools Record methods.
    final Method[] methods = PTStandaloneRecord.class.getMethods();
    ptMethodTable = new HashMap<String, Method>();
    for (Method m : methods) {
      if (m.getName().indexOf(PT_METHOD_PREFIX) == 0) {
        ptMethodTable.put(m.getName().substring(
            PT_METHOD_PREFIX.length()), m);
      }
    }
  }

  public PTStandaloneRecord(final PTRecordTypeConstraint origTc,
      final PTStandaloneRow pRow, final Record r) {
    super(origTc);
    this.parentRow = pRow;
    this.recDefn = r;
    this.init();
  }

  public PTStandaloneRecord(final PTRecordTypeConstraint origTc,
      final PTStandaloneRow pRow, final RecordBuffer recBuffer) {
    super(origTc);
    this.parentRow = pRow;
    this.recDefn = recBuffer.getRecDefn();
    this.recBuffer = recBuffer;
    this.init();
  }

  private void init() {
    // this map is linked in order to preserve
    // the order in which fields are added.
    this.fieldRefs = new LinkedHashMap<String, PTImmutableReference<PTStandaloneField>>();
    this.fieldRefIdxTable =
        new LinkedHashMap<Integer, PTImmutableReference<PTStandaloneField>>();
    int i = 1;
    for (final RecordField rf : this.recDefn.getExpandedFieldList()) {
      PTFieldTypeConstraint fldTc = new PTFieldTypeConstraint();

      try {
        PTImmutableReference<PTStandaloneField> newFldRef = null;

        // If this record field has a buffer associated with it, allocate the
        // field with that to give the field a reference to that buffer.
        if (this.recBuffer != null
            && this.recBuffer.hasRecordFieldBuffer(rf.FIELDNAME)) {
          throw new OPSVMachRuntimeException("MQUINN 11-30-2014 : Disabling for split.");
/*          newFldRef
            = new PTImmutableReference<PTField>(fldTc,
                fldTc.allocBufferField(this, this.recBuffer.getRecordFieldBuffer(rf.FIELDNAME)));*/
        } else {
          newFldRef
            = new PTImmutableReference<PTStandaloneField>(fldTc, fldTc.allocStandaloneField(this, rf));
        }
        this.fieldRefs.put(rf.FIELDNAME, newFldRef);
        this.fieldRefIdxTable.put(i++, newFldRef);
      } catch (final OPSTypeCheckException opstce) {
        throw new OPSVMachRuntimeException(opstce.getMessage(), opstce);
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder(super.toString());
    b.append(":").append(this.recDefn.RECNAME);
    b.append(",fieldRefs=").append(this.fieldRefs);
    return b.toString();
  }
}