/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.types;

import java.lang.reflect.Method;

import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.openpplsoft.buffers.*;
import org.openpplsoft.pt.*;
import org.openpplsoft.runtime.*;
import org.openpplsoft.sql.*;
import org.openpplsoft.trace.*;

/**
 * Represents a PeopleTools standalone (not in component buffer) rowset.
 */
public final class PTStandaloneRowset extends PTRowset<PTStandaloneRow> {

  private static Logger log = LogManager.getLogger(
      PTStandaloneRowset.class.getName());

  private static Map<String, Method> ptMethodTable;

  // If this is null, this rowset is a standalone rowset.
  private ScrollBuffer cBufferScrollDefn;

  static {
    final String PT_METHOD_PREFIX = "PT_";
    // cache pointers to PeopleTools Rowset methods.
    final Method[] methods = PTStandaloneRowset.class.getMethods();
    ptMethodTable = new HashMap<String, Method>();
    for (Method m : methods) {
      if (m.getName().indexOf(PT_METHOD_PREFIX) == 0) {
        ptMethodTable.put(m.getName().substring(
            PT_METHOD_PREFIX.length()), m);
      }
    }

    // Add the universal methods defined in the superclass as well.
    ptMethodTable.putAll(PTRowset.getUniversalRowsetMethodTable());
  }

  /**
   * Remember: the provided primary record defn could be null if
   * this rowset represents the level 0 scroll of the component buffer.
   */
  public PTStandaloneRowset(final PTRowsetTypeConstraint origTc, final PTStandaloneRow pRow,
      final Record primRecDefn) {
    super(origTc);
    this.parentRow = pRow;
    this.primaryRecDefn = primRecDefn;
    this.initRowset();
  }

  public PTStandaloneRowset(final PTRowsetTypeConstraint origTc, final PTStandaloneRow pRow,
      final ScrollBuffer scrollDefn) {
    super(origTc);
    this.parentRow = pRow;
    this.cBufferScrollDefn = scrollDefn;
    this.primaryRecDefn = scrollDefn.getPrimaryRecDefn();
    this.initRowset();
  }

  private void initRowset() {
    // One row is always present in the rowset, even when flushed.
    this.rows.add(this.allocateNewRow());
    this.registerRecordDefn(this.primaryRecDefn);
  }

  protected PTStandaloneRow allocateNewRow() {
    return new PTRowTypeConstraint().allocStandaloneRow(
        this, this.registeredRecordDefns, this.registeredChildScrollDefns);
  }

  @Override
  public Callable dotMethod(final String s) {
    if (ptMethodTable.containsKey(s)) {
      return new Callable(ptMethodTable.get(s), this);
    }
    return null;
  }

  public void registerRecordDefn(final Record recDefn) {

    if (recDefn == null) {
      return;
    }

    this.registeredRecordDefns.add(recDefn);

    // Each row must also have this record registered.
    for (final PTRow row : this.rows) {

      // If this is a component buffer scroll and the record has an
      // associated record buffer, pass that to the row; it will register
      // the underlying record defn and save a reference to that buffer
      if (this.cBufferScrollDefn != null
          && this.cBufferScrollDefn.hasRecordBuffer(recDefn.RECNAME)) {
/*        row.registerRecordDefn(
            this.cBufferScrollDefn.getRecordBuffer(recDefn.RECNAME));*/
          throw new OPSVMachRuntimeException("If you are seeing this exception, "
              + "it is because the above incorrect code has been commented out"
              + " and needs to be replaced now.");
      } else {
        row.registerRecordDefn(recDefn);
      }
    }
  }

  public void registerChildScrollDefn(final ScrollBuffer childScrollDefn) {
    if (this.registeredChildScrollDefns.containsKey(
        childScrollDefn.getPrimaryRecName())) {
      throw new OPSVMachRuntimeException("Halting on call to register child "
          + "scroll defn with a primary record name that has already been registerd; "
          + "registering it again would overwrite a potentially different defn.");
    } else {
      this.registeredChildScrollDefns.put(
          childScrollDefn.getPrimaryRecName(), childScrollDefn);
    }

    for (final PTRow row : this.rows) {
      row.registerChildScrollDefn(childScrollDefn);
    }
  }

  /**
   * MQUINN 12-03-2014 : Remove after split.
   */
  public void fireEvent(final PCEvent event,
      final FireEventSummary fireEventSummary) {}
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
  public void emitScrolls(final String indent) {}
  public void runFieldDefaultProcessing(
      final FieldDefaultProcSummary fldDefProcSummary) {}
  public ScrollBuffer getCBufferScrollDefn() {
    return null;
  }
  public void generateKeylist(
      final String fieldName, final Keylist keylist) {}
  public int getIndexOfRow(final PTRow row) {
    return -5;
  }
  public int determineScrollLevel() {
    return -5;
  }

  /**
   * Fill the rowset; the WHERE clause to use must be passed on the
   * OPS runtime stack.
   */
  public void PT_Fill() {
    final List<PTType> args = Environment.getDereferencedArgsFromCallStack();

    // If no args are provided to Fill, use a single blank as the where
    // clause.
    String whereClause = " ";
    String[] bindVals = new String[0];
    if (args.size() > 0) {
      whereClause = ((PTString) args.get(0)).read();

      // Gather bind values following the WHERE string on the stack.
      bindVals = new String[args.size() - 1];
      for (int i = 1; i < args.size(); i++) {
        final PTPrimitiveType bindExpr =
            Environment.getOrDerefPrimitive(args.get(i));
        bindVals[i - 1] = bindExpr.readAsString();
        //log.debug("Fill query bind value {}: {}", i-1, bindVals[i-1]);
      }
    }

    // The rowset must be flushed before continuing.
    this.internalFlush();

    final OPSStmt ostmt = StmtLibrary.prepareFillStmt(
        this.primaryRecDefn, whereClause, bindVals);
    OPSResultSet rs = ostmt.executeQuery();

    final List<RecordField> rfList = this.primaryRecDefn.getExpandedFieldList();
    final int numCols = rs.getColumnCount();

    if (numCols != rfList.size()) {
      throw new OPSVMachRuntimeException("The number of columns returned "
          + "by the fill query (" + numCols + ") differs from the number "
          + "of fields (" + rfList.size()
          + ") in the record defn field list.");
    }

    int rowsRead = 0;
    while (rs.next()) {

      //If at least one row exists, remove the empty row.
      if (rowsRead == 0) {
        this.rows.clear();
      }

      final PTStandaloneRow newRow = this.allocateNewRow();
      rs.readIntoRecord(newRow.getRecord(this.primaryRecDefn.RECNAME));
      this.rows.add(newRow);
      rowsRead++;
    }

    rs.close();
    ostmt.close();

    // Return the number of rows read from the fill operation.
    Environment.pushToCallStack(new PTInteger(rowsRead));
  }

  @Override
  public String toString() {
    final StringBuilder b = new StringBuilder(super.toString());
    if(this.primaryRecDefn == null) {
      b.append("!(CBUFFER-SCROLL-LEVEL-0-ROWSET)!");
    }
    b.append(":primaryRecDefn=").append(this.primaryRecDefn);
    b.append(",numRows=").append(this.rows.size());
    b.append(",registeredRecordDefns=").append(this.registeredRecordDefns);
    return b.toString();
  }
}
