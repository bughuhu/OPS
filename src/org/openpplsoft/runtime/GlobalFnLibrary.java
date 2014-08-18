/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.runtime;

import java.sql.*;
import java.util.*;
import java.util.regex.*;

import org.openpplsoft.sql.*;
import org.openpplsoft.pt.*;
import org.openpplsoft.buffers.*;
import org.openpplsoft.types.*;
import org.apache.logging.log4j.*;

public class GlobalFnLibrary {

  private static Logger log = LogManager.getLogger(GlobalFnLibrary.class.getName());

  /*
   * This is a shared function used by logical PeopleCode functions (tests for
   * blank values).
   * IMPORTANT: Use this: http://it.toolbox.com/blogs/spread-knowledge/understanding-blanknull-field-values-for-using-with-all-and-none-peoplecode-functions-40672
   * as a reference for null/blank rules with PeopleSoft data types.
   */
  private static boolean doesContainValue(PTType p) {
    if(p instanceof PTField) {
      return doesContainValue(((PTField)p).getValue());
    } else if(p instanceof PTString) {
      return ((PTString)p).read() != null && !((PTString)p).read().equals(" ");
    } else {
      throw new OPSVMachRuntimeException("Unexpected data type passed " +
        "to doesContainValue(ptdt).");
    }
  }

  /*==================================*/
  /* Global system functions          */
  /*==================================*/

  /*
   * Return true if none of the specified fields contain a value, return false
   * if one or more contain a value.
   */
  public static void PT_None() {
    for(PTType arg : Environment.getArgsFromCallStack()) {
      if(doesContainValue(arg)) {
        Environment.pushToCallStack(Environment.FALSE);
        return;
      }
    }
    Environment.pushToCallStack(Environment.TRUE);
  }

  /*
   * Return true if all of the specified fields contain a value, return false
   * if one or more do not.
   */
  public static void PT_All() {
    for(PTType arg : Environment.getArgsFromCallStack()) {
      if(!doesContainValue(arg)) {
        Environment.pushToCallStack(Environment.FALSE);
        return;
      }
    }
    Environment.pushToCallStack(Environment.TRUE);
  }

  public static void PT_Hide() {
    Environment.getArgsFromCallStack();
    // Not yet implemented.
  }

  public static void PT_SetSearchDialogBehavior() {
    Environment.getArgsFromCallStack();
    // Not yet implemented.
  }

  public static void PT_AllowEmplIdChg() {
    Environment.getArgsFromCallStack();
    // Not yet implemented.
  }

  public static void PT_Rept() {

    List<PTType> args = Environment.getArgsFromCallStack();
    if(args.size() != 2) {
      throw new OPSVMachRuntimeException("Expected two args.");
    }

    PTString str = (PTString)args.get(0);
    PTInteger reptNum = (PTInteger)args.get(1);

    StringBuilder b = new StringBuilder();
    for(int i = 0; i < reptNum.read(); i++) {
      b.append(str.read());
    }

    Environment.pushToCallStack(Environment.getFromLiteralPool(
      b.toString()));
    }

  public static void PT_Len() {

    List<PTType> args = Environment.getArgsFromCallStack();
    if(args.size() != 1 && !(args.get(0) instanceof PTString)) {
      throw new OPSVMachRuntimeException("Expected single string arg.");
    }

    Environment.pushToCallStack(Environment.getFromLiteralPool(
      ((PTString)args.get(0)).read().length()));
  }

  /*
   * TODO: Return true if DoModalComponent
   * has been previously called; requires more research.
   */
  public static void PT_IsModalComponent() {

    List<PTType> args = Environment.getArgsFromCallStack();
    if(args.size() != 0) {
      throw new OPSVMachRuntimeException("Expected zero arguments.");
    }
    Environment.pushToCallStack(Environment.FALSE);
  }

  public static void PT_CreateRecord() {

    List<PTType> args = Environment.getArgsFromCallStack();
    if(args.size() != 1 || (!(args.get(0) instanceof PTString))) {
      throw new OPSVMachRuntimeException("Expected single string arg.");
    }

    PTRecord rec = PTRecord.getSentinel().alloc(
        DefnCache.getRecord(((PTString)args.get(0)).read()));
    rec.setDefault();
    Environment.pushToCallStack(rec);
  }

  public static void PT_CreateRowset() {

    List<PTType> args = Environment.getArgsFromCallStack();
    if(args.size() != 1 || (!(args.get(0) instanceof PTString))) {
      throw new OPSVMachRuntimeException("Expected single string arg.");
    }

    Environment.pushToCallStack(PTRowset.getSentinel().alloc(
        DefnCache.getRecord(((PTString)args.get(0)).read())));
  }

  public static void PT_CreateArray() {

    /*
     * I am simply calling CreateArrayRept for now,
     * because I saw input for CreateArray(" ", 0) despite
     * the fact that the documentation says all arguments should
     * be the same type. In the future, there will likely be
     * instances where something other than CreateArrayRept
     * should be done.
     */
    PT_CreateArrayRept();
  }

  /**
   * Makes the Rowset object representing the level 0
   * component buffer available to caller.
   */
  public static void PT_GetLevel0() {

    List<PTType> args = Environment.getArgsFromCallStack();
    if(args.size() != 0) {
      throw new OPSVMachRuntimeException("Expected zero arguments.");
    }

    Environment.pushToCallStack(ComponentBuffer.ptGetLevel0());
  }

  public static void PT_CreateArrayRept() {

    List<PTType> args = Environment.getArgsFromCallStack();
    if(args.size() != 2 || (!(args.get(1) instanceof PTInteger))) {
      throw new OPSVMachRuntimeException("Expected two args, with the second "
          + "being an integer.");
    }

    int initialSize = ((PTInteger)args.get(1)).read();

    /*
     * If the type argument passed in is an array, create a new array
     * reference with a dimension that exceeds the array argument's by 1.
     * Otherwise create a one-dimension array.
     */
    PTArray newArray = null;
    if(args.get(0) instanceof PTArray) {
      newArray = PTArray.getSentinel(((PTArray)args.get(0)).dimensions + 1,
        (PTArray)args.get(0)).alloc();
    } else {
      newArray = PTArray.getSentinel(1, args.get(0)).alloc();
    }

    /*
     * IMPORTANT NOTE: If the type passed to this CreateArrayRept call is
     * an array, each iteration of the initialization loop should insert
     * a reference to that array into the array being initialized. The documentation
     * states that this is the behavior that occurs, unless .Clone() is used.
     */
    for(int i = 0; i < initialSize; i++) {
      throw new OPSVMachRuntimeException("Must support array instantiation in "+
        "CreateArrayRept; make sure to check toString() output.");
    }

    Environment.pushToCallStack(newArray);
  }

  public static void PT_IsMenuItemAuthorized() {

    List<PTType> args = Environment.getArgsFromCallStack();

    if(!(args.get(0) instanceof PTMenuLiteral)
        || !(args.get(1) instanceof PTMenuBarLiteral)
        || !(args.get(2) instanceof PTMenuItemLiteral)
        || !(args.get(3) instanceof PTPageLiteral)
        || !(args.get(4) instanceof PTString)) {
      throw new OPSVMachRuntimeException("The arguments provided to "
          + "IsMenuItemAuthorized do not match the expected types.");
    }

    String[] bindVals = {
      ((PTMenuLiteral) args.get(0)).getMenuName(),
      ((PTMenuBarLiteral) args.get(1)).getMenuBarName(),
      ((PTMenuItemLiteral) args.get(2)).getMenuItemName(),
      ((PTPageLiteral) args.get(3)).getPageName(),
      ((PTString) Environment.getSystemVar("%OperatorId")).read()
    };

    final String actionMode = ((PTString) args.get(4)).read();

    /*
     * First, get the menu defn provided and ensure that all of the
     * referenced component defns have been loaded into the defn cache.
     * At this time, nothing is done with these component defns within this
     * implementation of IsMenuItemAuthorized. However, this is clearly being
     * done by the PT implementation, and since component defn SQL is enforced,
     * it must also be done here as well in order to pass tracefile verification.
     * TODO(mquinn): Note that this can potentially be skipped if/when running in
     * an optimized mode that does not verify against a tracefile.
     */
    final Menu menuDefn = DefnCache.getMenu(bindVals[0]);
    menuDefn.loadReferencedComponents();

    /*
     * IMPORTANT NOTE:
     * The SQL retrieved here was handwritten by me (MQUINN) and not
     * based on anything found in a tracefile, since there are no SQL
     * stmts in/around the areas where IsMenuItemAuthorized is called in
     * the tracefiles I have at this point. Keep this in mind in the event
     * of future issues / changes.
     */
    OPSStmt ostmt = StmtLibrary.getStaticSQLStmt(
        "query.PSAUTHITEM_PSOPRCLS_IsMenuItemAuthorized",
        bindVals);
    ResultSet rs = null;

    try {
      rs = ostmt.executeQuery();

      /*
       * Iterate over each record in the resultset; we are looking for
       * the first one that allows the user to access the requested menu item
       * in the mode provided as an argument.
       * NOTE: I am checking simple equality b/w the mode arg and
       * AUTHORIZEDACTIONS for now. AUTHORIZEDACTIONS is actually a bit mask.
       * See the following for more information:
       * - http://www.erpassociates.com/peoplesoft-corner-weblog/security/secrets-of-psauthitem.html
       * - http://peoplesoftwiki.blogspot.com/2009/12/finding-barname-itemname-and-all-about.html
       * - http://peoplesoft.ittoolbox.com/groups/technical-functional/peopletools-l/how-to-interpret-authorizedactons-with-components-under-barname-3522093
       */
      while (rs.next()) {
        final String permList = rs.getString("PERMISSION_LIST_NAME");
        final int authorizedActions = rs.getInt("AUTHORIZEDACTIONS");

        log.debug("IsMenuItemAuthorized: Checking: "
            + "Permission List: {}; AUTHORIZEDACTIONS: {}",
            rs.getString("PERMISSION_LIST_NAME"),
            rs.getInt("AUTHORIZEDACTIONS"));

        /*
         * TODO(mquinn): This mapping is done adhoc right now, but once
         * more action modes are added, an enum should be use; see the links
         * listed above for exact mappings.
         */
        if (authorizedActions == 3 && actionMode.equals("U")) {
          log.debug("IsMenuItemAuthorized: found permissible record, returning True.");
          Environment.pushToCallStack(Environment.TRUE);
          return;
        }
      }
    } catch (final java.sql.SQLException sqle) {
      log.fatal(sqle.getMessage(), sqle);
      System.exit(ExitCode.GENERIC_SQL_EXCEPTION.getCode());
    } finally {
      try {
        if (rs != null) { rs.close(); }
        if (ostmt != null) { ostmt.close(); }
      } catch (final java.sql.SQLException sqle) {
        log.warn("Unable to close rs and/or ostmt in finally block.");
      }
    }

    // If no record permitted access for the given actionMode,
    // access to the menu item is not authorized.
    log.debug("IsMenuItemAuthorized: no permissible records found,"
      + " returning False.");
    Environment.pushToCallStack(Environment.FALSE);
  }

  public static void PT_MsgGetText() {
    List<PTType> args = Environment.getArgsFromCallStack();
    throw new OPSVMachRuntimeException("TODO: Implement MsgGetText");
  }

  /*==================================*/
  /* Shared OPS functions             */
  /*==================================*/

  public static void readRecordFromResultSet(Record recDefn,
      PTRecord recObj, ResultSet rs) throws SQLException {

    ResultSetMetaData rsMetadata = rs.getMetaData();
    int numCols = rsMetadata.getColumnCount();

    for(int i = 1; i <= numCols; i++) {
      String colName = rsMetadata.getColumnName(i);
      String colTypeName = rsMetadata.getColumnTypeName(i);
      PTField fldObj = recObj.getField(colName);
      GlobalFnLibrary.readFieldFromResultSet(fldObj,
        colName, colTypeName, rs);
    }
  }

  public static void readFieldFromResultSet(PTField fldObj,
      String colName, String colTypeName, ResultSet rs) throws SQLException {

    log.debug("Copying {} with type {} from resultset to Field:{} "+
        "with type flag {}", colName, colTypeName,
        fldObj.recFieldDefn.FIELDNAME, fldObj.getValue().getType());

    switch(fldObj.getValue().getType()) {
      /*
       * TODO(mquinn): Read CLOB and chars from ResultSet rather than
       * as string.
       */
      case CHAR:
        if(colTypeName.equals("CHAR") || colTypeName.equals("VARCHAR2")) {
          ((PTChar) fldObj.getValue()).write(
            rs.getString(colName));
        } else {
          throw new OPSVMachRuntimeException("Unexpected db " +
            "type for Type.CHAR: " + colTypeName + "; " +
            "colName=" + colName);
        }
        break;
      case STRING:
        if(colTypeName.equals("VARCHAR2") || colTypeName.equals("CLOB")
            || colTypeName.equals("CHAR")) {
          ((PTString) fldObj.getValue()).write(
            rs.getString(colName));
        } else {
          throw new OPSVMachRuntimeException("Unexpected db " +
            "type for Type.STRING: " + colTypeName + "; " +
            "colName=" + colName);
        }
        break;
      case NUMBER:
        if(colTypeName.equals("NUMBER")) {
          if(rs.getDouble(colName) % 1 == 0) {
            ((PTNumber)fldObj.getValue()).write(
              rs.getInt(colName));
          } else {
            ((PTNumber)fldObj.getValue()).write(
              rs.getDouble(colName));
          }
        } else {
          throw new OPSVMachRuntimeException("Unexpected db " +
            "type for Type.NUMBER: " + colTypeName + "; " +
            "colName=" + colName);
        }
        break;
      case DATE:
        if(colTypeName.equals("VARCHAR2")) {
          ((PTDate)fldObj.getValue()).write(
            rs.getString(colName));
        } else {
          throw new OPSVMachRuntimeException("Unexpected db " +
            "type for Type.DATE: " + colTypeName + "; " +
            "colName=" + colName);
        }
        break;
      case DATETIME:
        /*
         * TODO(mquinn): May need to be split apart into separate
         * statements with different types.
         */
        if(colTypeName.equals("VARCHAR2") || colTypeName.equals("TIMESTAMP")) {
          ((PTDateTime)fldObj.getValue()).write(
            rs.getString(colName));
        } else {
          throw new OPSVMachRuntimeException("Unexpected db " +
            "type for Type.DATETIME: " + colTypeName + "; " +
            "colName=" + colName);
        }
        break;
      default:
        throw new OPSVMachRuntimeException("Unexpected field " +
          "value type encountered when filling rowset: " +
           fldObj.getValue().getType());
    }
  }
}
