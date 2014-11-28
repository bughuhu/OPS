/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.buffers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openpplsoft.pt.*;
import org.openpplsoft.pt.pages.*;
import org.openpplsoft.runtime.*;
import org.openpplsoft.types.*;

/**
 * Represents a PeopleTools scroll buffer.
 */
public class ScrollBuffer implements IStreamableBuffer {

  private int scrollLevel;
  private String primaryRecName;
  private ScrollBuffer parent;
  private Map<String, ScrollBuffer> scrollBufferTable;
  private List<ScrollBuffer> orderedScrollBuffers;

  // We can't (shouldn't, since the complexity isn't worth it)
  // have an accompanying hash table here, because
  // multiple buffers for a record are allowed (reldisp records).
  private List<RecordBuffer> orderedRecBuffers;

  // Used for reading.
  private boolean hasEmittedSelf;
  private int recBufferCursor, scrollBufferCursor;

  /**
   * Constructs a scroll buffer with a particular scroll level, primary
   * record name, and parent scroll buffer.
   * @param l the scroll level of the new scroll buffer
   * @param r the primary record name of the new scroll buffer
   * @param p the parent scroll buffer of the new scroll buffer
   */
  public ScrollBuffer(final int l, final String r, final ScrollBuffer p) {
    this.scrollLevel = l;
    this.primaryRecName = r;
    this.parent = p;

    this.orderedRecBuffers = new ArrayList<RecordBuffer>();
    this.scrollBufferTable = new HashMap<String, ScrollBuffer>();
    this.orderedScrollBuffers = new ArrayList<ScrollBuffer>();
  }

  public Record getPrimaryRecDefn() {
    Record primaryRecDefn = null;
    if (this.primaryRecName != null) {
      primaryRecDefn = DefnCache.getRecord(this.primaryRecName);
    }
    return primaryRecDefn;
  }

  public PTRowset allocRowset(final PTRow parentRow) {

    // Create a rowset with the supplied parent; this scroll buffer object
    // will be linked to the rowset, which will use the primary rec name
    // defined in this object as the primary rec defn.
    final PTRowset rowset = new PTRowsetTypeConstraint()
        .alloc(parentRow, this);

    // For each record buffer in this scroll, register the underlying record
    // defn in the rowset (and thus its newly created child row as well).
    for (final RecordBuffer recBuf : this.orderedRecBuffers) {
      rowset.registerRecordDefn(DefnCache.getRecord(recBuf.getRecName()));
    }

    // For each child scroll in this scroll, register it in the rowset
    // (and thus in its newly created child row as well, where this method
    // will be called to allocate a new child rowset).
    for (final ScrollBuffer scrollBuf : this.orderedScrollBuffers) {
      rowset.registerChildScrollDefn(scrollBuf);
    }

    return rowset;
  }

  /**
   * Get the scroll level on which this scroll buffer exists.
   * @return this scroll buffer's scroll level
   */
  public int getScrollLevel() {
    return this.scrollLevel;
  }

  /**
   * Get the primary record name for this scroll buffer.
   * @return the primary record name for this scroll buffer.
   */
  public String getPrimaryRecName() {
    return this.primaryRecName;
  }

  /**
   * Get this scroll buffer's parent scroll buffer.
   * @return this scroll buffer's parent scroll buffer
   */
  public ScrollBuffer getParentScrollBuffer() {
    return this.parent;
  }

  /**
   * Add a page field to this scroll buffer.
   * @param tok the page field token representing the page field
   *    to be added.
   */
  public void addPageField(final PgToken tok) {

    RecordBuffer r = null;

    // Search for the first (may need to look for most recently used)
    // record buffer w/ the same record name as the token.
    for (final RecordBuffer rbuf : this.orderedRecBuffers) {
      if (rbuf.getRecName().equals(tok.RECNAME)) {
        r = rbuf;
        break;
      }
    }

    if (r == null) {
      r = new RecordBuffer(this, tok.RECNAME, this.scrollLevel,
          this.primaryRecName);
      this.orderedRecBuffers.add(r);
    }
    r.addPageField(tok.RECNAME, tok.FIELDNAME, tok);
  }

  /**
   * Gets a child scroll buffer given the name of the primary record
   * belonging to the desired child scroll buffer.
   * @param targetPrimaryRecName the primary record name attached to the
   *    desired scroll
   * @return the child scroll buffer with a primary record name matching
   *    the provided record name
   */
  public ScrollBuffer getChildScroll(final String targetPrimaryRecName) {
    ScrollBuffer sb = this.scrollBufferTable.get(targetPrimaryRecName);
    if (sb == null) {
      sb = new ScrollBuffer(this.scrollLevel + 1, targetPrimaryRecName, this);
      this.scrollBufferTable.put(targetPrimaryRecName, sb);
      this.orderedScrollBuffers.add(sb);
    }
    return sb;
  }

  public List<RecordBuffer> getOrderedRecBuffers() {
    return this.orderedRecBuffers;
  }

  public List<ScrollBuffer> getOrderedScrollBuffers() {
    return this.orderedScrollBuffers;
  }

  /**
   * Gets the next child buffer in the read sequence.
   * @return the next child buffer in the read sequence
   */
  public IStreamableBuffer next() {

    if (!this.hasEmittedSelf) {
      this.hasEmittedSelf = true;
      return this;
    }

    if (this.recBufferCursor < this.orderedRecBuffers.size()) {
      final RecordBuffer rbuf =
          this.orderedRecBuffers.get(this.recBufferCursor);
      final IStreamableBuffer toRet = rbuf.next();
      if (toRet != null) {
        return toRet;
      } else {
        this.recBufferCursor++;
        return this.next();
      }
    }

    if (this.scrollBufferCursor < this.orderedScrollBuffers.size()) {
      final ScrollBuffer sbuf =
          this.orderedScrollBuffers.get(this.scrollBufferCursor);
      final IStreamableBuffer toRet = sbuf.next();
      if (toRet != null) {
        return toRet;
      } else {
        this.scrollBufferCursor++;
        return this.next();
      }
    }

    return null;
  }

  /**
   * Resets the read cursors on this, and (recursively) on all child
   * buffers.
   */
  public void resetCursors() {

    this.hasEmittedSelf = false;
    this.recBufferCursor = 0;
    this.scrollBufferCursor = 0;

    for (RecordBuffer rbuf : this.orderedRecBuffers) {
      rbuf.resetCursors();
    }

    for (ScrollBuffer sbuf : this.orderedScrollBuffers) {
      sbuf.resetCursors();
    }
  }
}
