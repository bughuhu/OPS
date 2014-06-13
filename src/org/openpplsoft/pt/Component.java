/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.pt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.openpplsoft.buffers.*;
import org.openpplsoft.pt.pages.*;
import org.openpplsoft.pt.peoplecode.*;
import org.openpplsoft.runtime.*;
import org.openpplsoft.sql.StmtLibrary;
import org.openpplsoft.types.*;

/**
 * Represents a PeopleTools component definition.
 */
public class Component {

  private static Logger log = LogManager.getLogger(Component.class.getName());

  private String ptPNLGRPNAME;
  private String ptMARKET;

  // search record used when in add mode
  private String ptADDSRCHRECNAME;
  // name of non-add search record for this component
  private String ptSEARCHRECNAME;
  // 4-bit mask of allowed component modes
  private int ptACTIONS;
  // 0 - 2: New, Search, Keyword Search
  private int ptPRIMARYACTION;
  // 0 - 3: The mode used when ptPRIMARYACTION is Search
  private int ptDFLTACTION;
  // based on mode component is running in.
  private String searchRecordToUse;

  private List<Page> pages;
  private List<ComponentPeopleCodeProg> orderedComponentProgs;
  private boolean hasListOfComponentPCBeenRetrieved;

  /**
   * Creates a representation of the component with the given name
   * and market.
   * @param pnlgrpname the name of the component
   * @param market the market to which the component belongs
   */
  public Component(final String pnlgrpname, final String market) {

    this.ptPNLGRPNAME = pnlgrpname;
    this.ptMARKET = market;

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      pstmt = StmtLibrary.getPSPNLGRPDEFN(this.ptPNLGRPNAME, this.ptMARKET);
      rs = pstmt.executeQuery();
      rs.next();
      this.ptADDSRCHRECNAME = rs.getString("ADDSRCHRECNAME");
      this.ptSEARCHRECNAME = rs.getString("SEARCHRECNAME");
      this.ptACTIONS = rs.getInt("ACTIONS");
      this.ptPRIMARYACTION = rs.getInt("PRIMARYACTION");
      this.ptDFLTACTION = rs.getInt("DFLTACTION");
      rs.close();
      pstmt.close();

      /*
       * Select the search record to use based on the mode
       * the component should open in.
       */
      if (this.ptPRIMARYACTION == PSDefn.PRIMARYACTION_NEW) {
        this.searchRecordToUse = this.ptADDSRCHRECNAME;
      } else if (this.ptPRIMARYACTION == PSDefn.PRIMARYACTION_SEARCH) {
        this.searchRecordToUse = this.ptSEARCHRECNAME;
      } else {
        throw new OPSVMachRuntimeException("Unable to select search record "
            + "due to unknown Primary Action value.");
      }

      this.pages = new ArrayList<Page>();
      pstmt = StmtLibrary.getPSPNLGROUP(this.ptPNLGRPNAME, this.ptMARKET);
      rs = pstmt.executeQuery();
      while (rs.next()) {
        // All pages at the root of the component start at scroll level 0.
        final Page p = new Page(rs.getString("PNLNAME"));
        log.debug("Component contains Page.{}", p.getPNLNAME());
        this.pages.add(p);
      }
    } catch (final java.sql.SQLException sqle) {
      log.fatal(sqle.getMessage(), sqle);
      System.exit(ExitCode.GENERIC_SQL_EXCEPTION.getCode());
    } finally {
      try {
        if (rs != null) { rs.close(); }
        if (pstmt != null) { pstmt.close(); }
      } catch (final java.sql.SQLException sqle) {
        log.warn("Unable to close rs and/or pstmt in finally block.");
      }
    }
  }

  /**
   * Retrieves the list of PeopleCode programs attached to this
   * component, and caches the corresponding definition for each.
   */
  public void getListOfComponentPC() {

    if (this.hasListOfComponentPCBeenRetrieved) { return; }
    this.hasListOfComponentPCBeenRetrieved = true;

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      this.orderedComponentProgs = new ArrayList<ComponentPeopleCodeProg>();
      pstmt = StmtLibrary.getPSPCMPROG_CompPCList(PSDefn.COMPONENT,
          this.ptPNLGRPNAME, PSDefn.MARKET, this.ptMARKET);
      rs = pstmt.executeQuery();

      while (rs.next()) {
        final String objectid3 = rs.getString("OBJECTID3").trim();
        final String objectval3 = rs.getString("OBJECTVALUE3").trim();
        final String objectid4 = rs.getString("OBJECTID4").trim();
        final String objectval4 = rs.getString("OBJECTVALUE4").trim();
        final String objectid5 = rs.getString("OBJECTID5").trim();
        final String objectval5 = rs.getString("OBJECTVALUE5").trim();

        PeopleCodeProg prog = null;

        // Example: SSS_STUDENT_CENTER.GBL.PreBuild
        if (objectid3.equals(PSDefn.EVENT)) {
          prog = new ComponentPeopleCodeProg(this.ptPNLGRPNAME, this.ptMARKET,
            objectval3);

        // Example: SSS_STUDENT_CENTER.LS_SS_PERS_SRCH.SearchInit
        } else if (objectid3.equals(PSDefn.RECORD)
              && objectid4.equals(PSDefn.EVENT)) {
          prog = new ComponentPeopleCodeProg(this.ptPNLGRPNAME, this.ptMARKET,
            objectval3, objectval4);

        // Example: SSS_STUDENT_CENTER.LS_DERIVED_SSS_SCL
        //            .SS_CLS_SCHED_LINK.FieldChange
        } else if (objectid3.equals(PSDefn.RECORD)
              && objectid4.equals(PSDefn.FIELD)
              && objectid5.equals(PSDefn.EVENT)) {
          prog = new ComponentPeopleCodeProg(this.ptPNLGRPNAME, this.ptMARKET,
            objectval3, objectval4, objectval5);

        } else {
          throw new OPSVMachRuntimeException("Unexpected type of "
              + "Component PC encountered.");
        }

        prog = DefnCache.getProgram(prog);
        this.orderedComponentProgs.add((ComponentPeopleCodeProg) prog);
      }
    } catch (final java.sql.SQLException sqle) {
      log.fatal(sqle.getMessage(), sqle);
      System.exit(ExitCode.GENERIC_SQL_EXCEPTION.getCode());
    } finally {
      try {
        if (rs != null) { rs.close(); }
        if (pstmt != null) { pstmt.close(); }
      } catch (final java.sql.SQLException sqle) {
        log.warn("Unable to close rs and/or pstmt in finally block.");
      }
    }
  }

  /**
   * If the search record for this component contains one or more keys,
   * execute the SearchInit Record PeopleCode program attached to
   * the record if it exists.
   */
  public void loadAndRunRecordPConSearchRecord() {

    final Record recDefn = DefnCache.getRecord(this.searchRecordToUse);
    ComponentBuffer.setSearchRecord(PTRecord.getSentinel().alloc(recDefn));

    if (!recDefn.hasAnyKeys()) {
      log.debug("No keys on search record.");
      return;
    }

    recDefn.discoverRecordPC();
    for (PeopleCodeProg prog : recDefn.orderedRecordProgs) {
      if (prog.event.equals("SearchInit")) {
        final PeopleCodeProg p = DefnCache.getProgram(prog);
        final ExecContext eCtx = new ProgramExecContext(p);
        final InterpretSupervisor interpreter = new InterpretSupervisor(eCtx);
        interpreter.run();
      }
    }
  }

  /**
   * Run any and all Component PeopleCode programs attached to the
   * search record for this component.
   */
  public void loadAndRunComponentPConSearchRecord() {

    for (ComponentPeopleCodeProg prog : this.orderedComponentProgs) {
      if (prog.RECNAME != null && prog.RECNAME.equals(this.searchRecordToUse)) {
        final PeopleCodeProg p = DefnCache.getProgram(prog);
        final ExecContext eCtx = new ProgramExecContext(p);
        final InterpretSupervisor interpreter = new InterpretSupervisor(eCtx);
        interpreter.run();
      }
    }
  }

  /**
   * If the search record contains at least one key, fill the
   * search record with data.
   */
  public void fillSearchRecord() {

    final Record recDefn = DefnCache.getRecord(this.searchRecordToUse);

    if (!recDefn.hasAnyKeys()) {
      return;
    }

    PreparedStatement pstmt = null;
    ResultSet rs = null;

    try {
      pstmt = StmtLibrary.getSearchRecordFillQuery();
      rs = pstmt.executeQuery();

      final ResultSetMetaData rsMetadata = rs.getMetaData();
      final int numCols = rsMetadata.getColumnCount();

      // search record may legitimately be empty, check before continuing.
      if (rs.next()) {
        final PTRecord searchRecord = ComponentBuffer.getSearchRecord();
        for (int i = 1; i <= numCols; i++) {
          final String colName = rsMetadata.getColumnName(i);
          final String colTypeName = rsMetadata.getColumnTypeName(i);
          final PTField fldObj = searchRecord.getField(colName);
          GlobalFnLibrary.readFieldFromResultSet(fldObj,
              colName, colTypeName, rs);
        }
        if (rs.next()) {
          throw new OPSVMachRuntimeException(
              "Result set for search record fill has more than "
              + "one record.");
        }
      }
    } catch (final java.sql.SQLException sqle) {
      log.fatal(sqle.getMessage(), sqle);
      System.exit(ExitCode.GENERIC_SQL_EXCEPTION.getCode());
    } finally {
      try {
        if (rs != null) { rs.close(); }
        if (pstmt != null) { pstmt.close(); }
      } catch (final java.sql.SQLException sqle) {
        log.warn("Unable to close rs and/or pstmt in finally block.");
      }
    }
  }

  /**
   * Recursively loads the subpages and secpages attached to this
   * component.
   */
  public void loadPages() {
    for (Page p : this.pages) {
      p.recursivelyLoadSubpages();
    }

    for (Page p : this.pages) {
      p.recursivelyLoadSecpages();
    }
  }

  /**
   * Reads the stream of page fields for each page in this
   * component definition, then builds appropriate buffers
   * to create the complete component buffer.
   */
  public void assembleComponentStructure() {

    PgToken tok;
    PgTokenStream pfs;

    final byte REL_DISP_FLAG = (byte) 16;

    for (Page p : this.pages) {
      pfs = new PgTokenStream(p.getPNLNAME());

      final Stack<ScrollMarker> scrollMarkers = new Stack<ScrollMarker>();
      scrollMarkers.push(new ScrollMarker(0, null, PFlag.PAGE));

      while ((tok = pfs.next()) != null) {

        //log.debug(tok);

        if (tok.flags.contains(PFlag.PAGE)) {
          final ScrollMarker sm = new ScrollMarker();
          sm.src = PFlag.PAGE;
          sm.primaryRecName = scrollMarkers.peek().primaryRecName;
          sm.scrollLevel = scrollMarkers.peek().scrollLevel;
          scrollMarkers.push(sm);
          continue;
        }

        if (tok.flags.contains(PFlag.END_OF_PAGE)) {
          while (scrollMarkers.peek().src == PFlag.SCROLL_START) {
            // pop interim scroll levels.
            scrollMarkers.pop();
          }
          // pop the matching page.
          scrollMarkers.pop();
          continue;
        }

        if (tok.flags.contains(PFlag.SCROLL_START)) {

          // This scroll may appear right after an unended scroll;
          // if so, pop the previous one.
          final ScrollMarker topSm = scrollMarkers.peek();
          if (topSm.src == PFlag.SCROLL_START
              && !tok.primaryRecName.equals(topSm.primaryRecName)) {
            scrollMarkers.pop();
          }

          final ScrollMarker sm = new ScrollMarker();
          sm.src = PFlag.SCROLL_START;
          sm.primaryRecName = tok.primaryRecName;
          sm.scrollLevel = scrollMarkers.peek().scrollLevel + tok.OCCURSLEVEL;
          scrollMarkers.push(sm);
          continue;
        }

        // Remember: don't "continue" here, since SCROLL_LVL_DECREMENT
        // can be attached to regular fields.
        if (tok.flags.contains(PFlag.SCROLL_LVL_DECREMENT)) {
          scrollMarkers.pop();
        }

        if (tok.doesBelongInComponentStructure()) {
          ComponentBuffer.addPageField(tok, scrollMarkers.peek().scrollLevel,
            scrollMarkers.peek().primaryRecName);
        }
      }

      if (scrollMarkers.size() != 0) {
        throw new OPSVMachRuntimeException("Scroll marker stack size "
            + "exceeds 0 at the end of the page token stream.");
      }
    }
  }

  private class ScrollMarker {
    private String primaryRecName;
    private int scrollLevel;
    private PFlag src;

    public ScrollMarker() {}

    public ScrollMarker(final int s, final String p, final PFlag a) {
      this.scrollLevel = s;
      this.primaryRecName = p;
      this.src = a;
    }
  }
}
