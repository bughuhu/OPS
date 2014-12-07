/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.types;

import java.lang.reflect.Method;

import java.util.Collection;
import java.util.ArrayList;
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
public abstract class PTRow<R extends PTRowset, E extends PTRecord>
    extends PTObjectType {

  private static Map<String, Method> ptMethodTable;

  private static final Logger log = LogManager.getLogger(PTRow.class.getName());

  protected R parentRowset;
  protected PTImmutableReference<PTBoolean> selectedPropertyRef;

  // Maps record names to child record objects
  protected Map<String, E> recordMap = new LinkedHashMap<String, E>();
  protected Set<Record> registeredRecordDefns = new HashSet<Record>();

  // Maps scroll/rowset primary rec names to rowset objects
  protected Map<String, R> rowsetMap = new LinkedHashMap<String, R>();
  protected Map<String, ScrollBuffer> registeredChildScrollDefns =
      new LinkedHashMap<String, ScrollBuffer>();

  static {
    final String PT_METHOD_PREFIX = "PT_";
    // cache pointers to PeopleTools Row methods.
    final Method[] methods = PTRow.class.getMethods();
    ptMethodTable = new HashMap<String, Method>();
    for (Method m : methods) {
      if (m.getName().indexOf(PT_METHOD_PREFIX) == 0) {
        ptMethodTable.put(m.getName().substring(
            PT_METHOD_PREFIX.length()), m);
      }
    }
  }

  public PTRow(final PTRowTypeConstraint origTc) {
    super(origTc);
  }

  public abstract void registerRecordDefn(final Record recDefn);
  public abstract void registerChildScrollDefn(final ScrollBuffer childScrollDefn);

  public R getParentRowset() {
    return this.parentRowset;
  }

  /**
   * Retrieve the record associated with the record name provided
   * @return the record associated with the record name provided
   */
  public E getRecord(final String recName) {
    return this.recordMap.get(recName);
  }

  /**
   * Retrieves the record at the given index (records are stored
   * in a linked hash map and thus are ordered).
   */
  public E getRecord(final int index) {
    final List<E> orderedList = new ArrayList<E>(this.recordMap.values());

    // Remember: PS uses 1-based indices, not 0-based, must adjust here.
    return orderedList.get(index - 1);
  }

  public R getRowset(final String primaryRecName) {
    return this.rowsetMap.get(primaryRecName);
  }

  /**
   * Determines if the given record exists in the row.
   * @return true if record exists, false otherwise
   */
  public boolean hasRecord(final String recName) {
    return this.recordMap.containsKey(recName);
  }

  /**
   * Implementation of GetRecord method for the PeopleTools
   * row class.
   */
  public void PT_GetRecord() {
    final List<PTType> args = Environment.getDereferencedArgsFromCallStack();
    if (args.size() != 1) {
      throw new OPSVMachRuntimeException("Expected only one arg.");
    }

    E rec = null;
    if(args.get(0) instanceof PTRecordLiteral) {
      rec = this.getRecord(((PTRecordLiteral) args.get(0)).read());
    } else if (args.get(0) instanceof PTInteger) {
      rec = this.getRecord(((PTInteger) args.get(0)).read());
    } else {
      throw new OPSVMachRuntimeException("Expected arg to GetRecord() to "
          + "be a PTRecordLiteral or PTInteger.");
    }

    Environment.pushToCallStack(rec);
  }

  @Override
  public PTType dotProperty(final String s) {
    if (this.recordMap.containsKey(s)) {
      return this.recordMap.get(s);
    } else if (s.toLowerCase().equals("recordcount")) {
      return new PTInteger(this.registeredRecordDefns.size());
    } else if (s.toLowerCase().equals("parentrowset")) {
      return this.parentRowset;
    } else if (s.toLowerCase().equals("selected")) {
      return this.selectedPropertyRef;
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

  public Map<String, E> getRecordMap() {
    return this.recordMap;
  }

  @Override
  public void setReadOnly() {
    super.setReadOnly();

    // Calls to make a row read-only must make its child records read-only.
    for(Map.Entry<String, E> cursor: this.recordMap.entrySet()) {
      cursor.getValue().setReadOnly();
    }

    // Calls to make a row read-only must make its child rowsets read-only.
    for(Map.Entry<String, R> cursor: this.rowsetMap.entrySet()) {
      cursor.getValue().setReadOnly();
    }
  }
}
