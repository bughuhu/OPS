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
public final class PTStandaloneRowset extends PTRowset {

  private static Logger log = LogManager.getLogger(
      PTStandaloneRowset.class.getName());

  private static Map<String, Method> ptMethodTable;

  private PTStandaloneRow parentRow;
  private List<PTStandaloneRow> rows = new ArrayList<PTStandaloneRow>();
  private Record primaryRecDefn;

  // If this is null, this rowset is a standalone rowset.
  private ScrollBuffer cBufferScrollDefn;

  private Set<Record> registeredRecordDefns = new HashSet<Record>();
  private Map<String, ScrollBuffer> registeredChildScrollDefns =
      new LinkedHashMap<String, ScrollBuffer>();

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
    this.rows.add(new PTRowTypeConstraint().allocStandaloneRow(
        this, this.registeredRecordDefns, this.registeredChildScrollDefns));
    this.registerRecordDefn(this.primaryRecDefn);
  }

  public void registerRecordDefn(final Record recDefn) {

    if (recDefn == null) {
      return;
    }

    this.registeredRecordDefns.add(recDefn);

    // Each row must also have this record registered.
    for (final PTStandaloneRow row : this.rows) {

      // If this is a component buffer scroll and the record has an
      // associated record buffer, pass that to the row; it will register
      // the underlying record defn and save a reference to that buffer
      if (this.cBufferScrollDefn != null
          && this.cBufferScrollDefn.hasRecordBuffer(recDefn.RECNAME)) {
        row.registerRecordDefn(
            this.cBufferScrollDefn.getRecordBuffer(recDefn.RECNAME));
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

    for (final PTStandaloneRow row : this.rows) {
      row.registerChildScrollDefn(childScrollDefn);
    }
  }

  public void fireEvent(final PCEvent event,
      final FireEventSummary fireEventSummary) {
    for (final PTStandaloneRow row : this.rows) {
      row.fireEvent(event, fireEventSummary);
    }
  }

  public PTRecord resolveContextualCBufferRecordReference(final String recName) {
    if (this.parentRow != null) {
      return this.parentRow.resolveContextualCBufferRecordReference(recName);
    }
    return null;
  }

  public PTReference<PTField> resolveContextualCBufferRecordFieldReference(
      final String recName, final String fieldName) {
    if (this.parentRow != null) {
      return this.parentRow.resolveContextualCBufferRecordFieldReference(
          recName, fieldName);
    }
    return null;
  }

  public PTRowset resolveContextualCBufferScrollReference(
      final PTScrollLiteral scrollName) {
    if (this.parentRow != null) {
      return this.parentRow.resolveContextualCBufferScrollReference(scrollName);
    }
    return null;
  }

  public void emitScrolls(final String indent) {
    for (int i = 0; i < this.rows.size(); i++) {
      this.rows.get(i).emitScrolls(indent);
    }
  }

  public void runFieldDefaultProcessing(
      final FieldDefaultProcSummary fldDefProcSummary) {
    for (final PTStandaloneRow row : this.rows) {
      row.runFieldDefaultProcessing(fldDefProcSummary);
    }
  }

  public ScrollBuffer getCBufferScrollDefn() {
    return this.cBufferScrollDefn;
  }

  /**
   * If a key lookup request reaches a Rowset, the request should
   * always be passed to the parent row in order to look at the child
   * records within that row.
   */
  public void generateKeylist(
      final String fieldName, final Keylist keylist) {
    if (this.parentRow != null) {
      this.parentRow.generateKeylist(fieldName, keylist);
    }
  }

  public PTStandaloneRow getParentRow() {
    return this.parentRow;
  }

  public int getActiveRowCount() {
    return this.rows.size();
  }

  @Override
  public PTType dotProperty(final String s) {
    if (s.toLowerCase().equals("activerowcount")) {
      return new PTInteger(this.getActiveRowCount());
    } else if (s.toLowerCase().equals("dbrecordname")) {
      return new PTString(this.primaryRecDefn.RECNAME);
    }

    return null;
  }

  @Override
  public Callable dotMethod(final String s) {
    if (ptMethodTable.containsKey(s)) {
      return new Callable(ptMethodTable.get(s), this);
    }
    return null;
  }

  /**
   * Get a row from the rowset; the index to use must be placed
   * on the OPS runtime stack.
   */
  public void PT_GetRow() {
    final List<PTType> args = Environment.getDereferencedArgsFromCallStack();
    if (args.size() != 1) {
      throw new OPSVMachRuntimeException("Expected only one arg.");
    }

    int idx = -1;
    final PTType iterExpr = args.get(0);

    if (iterExpr instanceof PTInteger) {
      idx = ((PTInteger) iterExpr).read();
    } else if (iterExpr instanceof PTNumber) {
      idx = ((PTNumber) iterExpr).readAsInteger();
    } else {
      throw new OPSVMachRuntimeException("Expected iterExpr to be"
          + " either PTInteger or PTNumber.");
    }

    if (idx < 1 || idx > this.rows.size()) {
      throw new OPSVMachRuntimeException("Index (" + idx + ") provided to "
          + "getRows is out of bounds; number of rows is "
          + this.rows.size());
    }

    Environment.pushToCallStack(this.getRow(idx));
  }

  /**
   * Return the row from the rowset corresponding to the given index;
   * PeopleSoft RowSet indices start from 1, not 0.
   * @param idx the index of the row to retrieve
   * @return the row corresponding to idx
   */
  public PTStandaloneRow getRow(int idx) {
    // Must subtract 1 from idx; rowset indices start at 1.
    return this.rows.get(idx - 1);
  }

  public int getIndexOfRow(final PTRow row) {
    for (int i = 0; i < this.rows.size(); i++) {
      if(row == this.rows.get(i)) {
        return i;
      }
    }
    throw new OPSVMachRuntimeException("Unable to get index for the provided row; "
        + "row does not exist in this rowset.");
  }

  public int determineScrollLevel() {
    if (this == ComponentBuffer.getCBufferRowset()) {
      return -1;
    } else if (this.parentRow != null) {
      return 1 + this.parentRow.determineScrollLevel();
    } else {
      throw new OPSVMachRuntimeException("Unable to determine scroll level "
          + "for this rowset; this is not the root cbuffer rowset, but "
          + "the parent row is null, so unable to continue traversal.");
    }
  }

  /**
   * Sort the rows in the rowset; the exact order ("A" for ascending,
   * "D" for descending) must be passed on the OPS runtime stack.
   * TODO(mquinn): Technically, rows can have multiple child records
   * and 0 to n child rowsets. Keep this in mind in the event of
   * future issues with this method, as I am deferring support for those
   * scenarios.
   */
  public void PT_Sort() {

    final LinkedList<String> fields = new LinkedList<String>();
    final LinkedList<String> orders = new LinkedList<String>();

    do {
      final PTString orderStr = (PTString) Environment.popFromCallStack();
      if (orderStr.read().equals("A") || orderStr.read().equals("D")){
        orders.push(orderStr.read());
      } else {
        throw new OPSVMachRuntimeException("Unexpected order "
          + "string: " + orderStr.read());
      }

      final PTRecordFieldSpecifier fldSpec =
          (PTRecordFieldSpecifier) Environment.popFromCallStack();
      if (!fldSpec.getRecName().equals(this.primaryRecDefn.RECNAME)) {
        throw new OPSVMachRuntimeException("Encountered a sort "
            + "field for a record other than the one underlying this "
            + "rowset; this is legal but not yet supported.");
      }

      if (!this.primaryRecDefn.fieldTable.containsKey(fldSpec.getFieldName())) {
        throw new OPSVMachRuntimeException("The field "
            + fldSpec.getFieldName() + " does not exist on the underlying "
            + "record.");
      }
      fields.push(fldSpec.getFieldName());

    } while (!(Environment.peekAtCallStack() instanceof PTCallFrameBoundary));

    this.rows = this.mergeSortRows(this.rows, fields, orders);

    /*int i=1;
    log.debug("=========== Sorted Rowset ===========");
    for(PTRow row : this.rows) {
      PTRecord rec = row.record;
      log.debug("{}: STRM={}, ACAD_CAREER={}, INSTITUTION={}",
        i++, rec.fields.get("STRM"), rec.fields.get("ACAD_CAREER"),
        rec.fields.get("INSTITUTION"));
    }
    log.debug("======== End Sorted Rowset =========");*/
  }

  private List<PTStandaloneRow> mergeSortRows(final List<PTStandaloneRow> rowsToSort,
    final List<String> sortFields, final List<String> sortOrders) {

    if (rowsToSort.size() < 2) {
      return rowsToSort;
    }

    final int mid = rowsToSort.size() / 2;
    final List<PTStandaloneRow> left = this.mergeSortRows(
        rowsToSort.subList(0, mid), sortFields, sortOrders);
    final List<PTStandaloneRow> right = this.mergeSortRows(
        rowsToSort.subList(mid,
          rowsToSort.size()), sortFields, sortOrders);
    return this.merge(left, right, sortFields, sortOrders);
  }

  private List<PTStandaloneRow> merge(final List<PTStandaloneRow> left,
      final List<PTStandaloneRow> right, final List<String> sortFields,
      final List<String> sortOrders) {

    final List<PTStandaloneRow> merged = new ArrayList<PTStandaloneRow>();
    int l = 0, r = 0;
    while (l < left.size() && r < right.size()) {
      final PTStandaloneRow lRow = left.get(l);
      final PTStandaloneRow rRow = right.get(r);

      /*
       * Order the rows based on the precedence
       * specified in the list of sort fields,
       * along with the order accompanying each (A or D).
       */
      for (int i = 0; i < sortFields.size(); i++) {
        final String order = sortOrders.get(i);

        final PTPrimitiveType lVal = lRow.getRecord(this.primaryRecDefn.RECNAME)
            .getFieldRef(sortFields.get(i)).deref().getValue();
        final PTPrimitiveType rVal = rRow.getRecord(this.primaryRecDefn.RECNAME)
            .getFieldRef(sortFields.get(i)).deref().getValue();

        if (lVal.isLessThan(rVal).read()) {
          if (order.equals("A")) {
            merged.add(lRow);
            l++;
          } else {
            merged.add(rRow);
            r++;
          }
          break;
        } else if (lVal.isGreaterThan(rVal).read()) {
          if (order.equals("A")) {
            merged.add(rRow);
            r++;
          } else {
            merged.add(lRow);
            l++;
          }
          break;
        } else {
          /*
           * If this is the last sort field,
           * and the rows are still considered "equal"
           * in terms of ordering, add both to the merged
           * array in terms of their natural ordering.
           */
          if ((i + 1) == sortFields.size()) {
            merged.add(lRow);
            l++;
            merged.add(rRow);
            r++;
          } else {
            /*
             * If another sort field exists,
             * use that to determine ordering.
             */
            continue;
          }
        }
      }
    }

    /*
     * Add any surplus elements to end of merged array.
     */
    while (l < left.size()) {
      merged.add(left.get(l++));
    }
    while (r < right.size()) {
      merged.add(right.get(r++));
    }

    return merged;
  }

  /**
   * Flush the rowset (only a default empty row will be present
   * after the flush).
   */
  public void PT_Flush() {
    final List<PTType> args = Environment.getArgsFromCallStack();
    if (args.size() != 0) {
      throw new OPSVMachRuntimeException("Expected zero arguments.");
    }
    this.internalFlush();
  }

  private void internalFlush() {
    // One row is always present in the rowset, even when flushed.
    this.rows.clear();
    this.rows.add(new PTRowTypeConstraint().allocStandaloneRow(
        this, this.registeredRecordDefns, this.registeredChildScrollDefns));
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

      final PTStandaloneRow newRow = new PTRowTypeConstraint().allocStandaloneRow(
          this, this.registeredRecordDefns, this.registeredChildScrollDefns);

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
