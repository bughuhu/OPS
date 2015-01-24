/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.pt.peoplecode;

import static java.util.stream.Collectors.toSet;

import java.sql.*;
import java.util.*;
import java.io.*;

import org.apache.logging.log4j.*;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.tree.*;

import org.openpplsoft.sql.*;
import org.openpplsoft.bytecode.*;
import org.openpplsoft.pt.*;
import org.openpplsoft.runtime.*;
import org.openpplsoft.antlr4.*;
import org.openpplsoft.antlr4.frontend.*;
import org.openpplsoft.types.*;

public abstract class PeopleCodeProg {

  private static Logger log = LogManager.getLogger(PeopleCodeProg.class.getName());

  protected String[] bindVals;
  protected String event;
  private String programText;
  private byte[] bytecode;
  private CommonTokenStream tokenStream;
  private ParseTree parseTree;
  private boolean hasAtLeastOneStatementFlag;

  private List<PeopleCodeProg> referencedProgs;
  private Map<String, RecordPeopleCodeProg> recordProgFnCalls;
  private Map<String, FuncImpl> funcImplNodes;
  private Map<String, Function> funcTable;
  private Map<Integer, BytecodeReference> bytecodeRefTable;
  private Map<String, List<AppPackagePath>> importedAppClasses;
  private List<AppPackagePath> importedAppPackagePaths;

  private boolean hasInitialized, haveLoadedDefnsAndPrograms, haveLexedAndParsed;

  public class FuncImpl {
    public String funcName;
    public PeopleCodeParser.FuncImplContext parseTreeNode;
    public FuncImpl(String fName, PeopleCodeParser.FuncImplContext node) {
      this.funcName = fName;
      this.parseTreeNode = node;
    }
  }

  public class Function {
    public String name;
    public List<FormalParam> formalParams;
    public PTTypeConstraint returnTypeConstraint;
    public Function(String n, List<FormalParam> l, PTTypeConstraint rtc) {
      this.name = n;
      this.formalParams = l;
      this.returnTypeConstraint = rtc;
    }
  }

  protected PeopleCodeProg() {}

  protected abstract void initBindVals();

  public abstract String getDescriptor();

  public void addImportedAppPackagePath(final AppPackagePath path) {
    this.importedAppPackagePaths.add(path);
  }

  public List<AppPackagePath> getImportedAppPackagePaths() {
    return this.importedAppPackagePaths;
  }

  public ParseTree getParseTree() {
    if(parseTree == null) {
      this.lexAndParse();
    }
    return this.parseTree;
  }

  public CommonTokenStream getTokenStream() {
    return this.tokenStream;
  }

  public String getEvent() {
    return this.event;
  }

  public byte[] getBytecode() {
    return this.bytecode;
  }

  public void init() {

    if(this.hasInitialized) { return; }
    this.hasInitialized = true;

    OPSStmt ostmt = StmtLibrary.getStaticSQLStmt("query.PSPCMPROG_GetPROGTXT",
        new String[]{this.bindVals[0], this.bindVals[1], this.bindVals[2],
        this.bindVals[3], this.bindVals[4], this.bindVals[5], this.bindVals[6],
        this.bindVals[7], this.bindVals[8], this.bindVals[9], this.bindVals[10],
        this.bindVals[11], this.bindVals[12], this.bindVals[13]});
    OPSResultSet rs = ostmt.executeQuery();

    /*
     * Append the program bytecode; there could be multiple records
     * for this program if the length exceeds 28,000 bytes. Note that
     * the above query must be ordered by PROSEQ, otherwise these records
     * will need to be pre-sorted before appending the BLOBs together.
     */
    int PROGLEN = -1;
    while(rs.next()) {
      PROGLEN = rs.getInt("PROGLEN");     // PROGLEN is the same for all records returned here.
      this.appendBytecode(rs.getBlob("PROGTXT"));
    }
    rs.close();
    ostmt.close();

    if(this.bytecode.length != PROGLEN) {
      throw new OPSVMachRuntimeException("Number of bytes in " + this.getDescriptor() + " ("
          + this.bytecode.length + ") not equal to PROGLEN (" + PROGLEN + ").");
    }

    this.bytecodeRefTable = new TreeMap<Integer, BytecodeReference>();

    ostmt = StmtLibrary.getStaticSQLStmt("query.PSPCMPROG_GetRefs",
        new String[]{this.bindVals[0], this.bindVals[1], this.bindVals[2],
        this.bindVals[3], this.bindVals[4], this.bindVals[5],
        this.bindVals[6], this.bindVals[7], this.bindVals[8],
        this.bindVals[9], this.bindVals[10], this.bindVals[11],
        this.bindVals[12], this.bindVals[13]});
    rs = ostmt.executeQuery();
    while(rs.next()) {

    /*if (rs.getString("RECNAME").equals("DERIVED_REGFRM1")
        && rs.getString("REFNAME").equals("SS_TRANSACT_TITLE")) {
      throw new OPSVMachRuntimeException("Found SS_TRANSACT_TITLE on: " + this);
    }*/

      this.bytecodeRefTable.put(rs.getInt("NAMENUM"),
          new BytecodeReference(
              rs.getInt("NAMENUM"),
              rs.getString("RECNAME").trim(),
              rs.getString("REFNAME").trim()));
    }
    rs.close();
    ostmt.close();

    /*
     * Assemble the text of the program from its constituent bytecode.
     */
    PeopleCodeByteStream byteStream = new PeopleCodeByteStream(this);
    Assembler a = new Assembler(byteStream);
    a.assemble();
    this.programText = byteStream.getAssembledText();
  }

  public void appendBytecode(final Blob blob) {

    int b;
    InputStream stream = null;
    ArrayList<Integer> bytes = new ArrayList<Integer>();

    try {
      stream = blob.getBinaryStream();
      while((b = stream.read()) != -1) {
        bytes.add(b);
      }
    } catch(final java.sql.SQLException sqle) {
      throw new OPSVMachRuntimeException(sqle.getMessage(), sqle);
    } catch(final java.io.IOException ioe) {
      throw new OPSVMachRuntimeException(ioe.getMessage(), ioe);
    } finally {
      try {
        if(stream != null) { stream.close(); }
      } catch(java.io.IOException ioe) {}
    }

    int startIdx;
    byte[] allBytes;

    if(this.bytecode == null) {
      allBytes = new byte[bytes.size()];
      startIdx = 0;
    } else {
      allBytes = new byte[this.bytecode.length + bytes.size()];
      startIdx = this.bytecode.length;

      // Copy previous bytes to new array.
      for(int i = 0; i < startIdx; i++) {
        allBytes[i] = this.bytecode[i];
      }
    }

    Iterator<Integer> iter = bytes.iterator();
    for(int i = startIdx; i < allBytes.length; i++) {
      allBytes[i] = iter.next().byteValue();
    }

    this.bytecode = allBytes;
  }

  public void loadDefnsAndPrograms() {
    log.debug("Loading defns and programs for {}", this.getDescriptor());

    if(this.haveLoadedDefnsAndPrograms) { return; }
    this.haveLoadedDefnsAndPrograms = true;

    this.referencedProgs = new ArrayList<PeopleCodeProg>();
    this.recordProgFnCalls = new HashMap<String, RecordPeopleCodeProg>();
    this.funcImplNodes = new HashMap<String, FuncImpl>();
    this.funcTable = new HashMap<String, Function>();
    this.importedAppClasses = new HashMap<String, List<AppPackagePath>>();
    this.importedAppPackagePaths = new ArrayList<AppPackagePath>();

    this.lexAndParse();
    ParseTreeWalker walker = new ParseTreeWalker();
    walker.walk(new ProgLoadListener(this), this.parseTree);
  }

  public void addRecordProgFnCall(final String fnName,
      final RecordPeopleCodeProg prog) {
    this.recordProgFnCalls.put(fnName, prog);
  }

  public void addReferencedProg(final PeopleCodeProg prog) {
    this.referencedProgs.add(prog);
  }

  public List<PeopleCodeProg> getReferencedProgs() {
    return this.referencedProgs;
  }

  public boolean hasRecordProgFnCall(final String fnName) {
    return this.recordProgFnCalls.containsKey(fnName);
  }

  public RecordPeopleCodeProg getRecordProgFnCall(final String fnName) {
    return this.recordProgFnCalls.get(fnName);
  }

  public BytecodeReference getBytecodeReference(int refNbr) {
    return this.bytecodeRefTable.get(refNbr);
  }

  public Set<String> getUniqueRecFieldRefsForPRMInclusion() {
    //log.debug("\n\n\n\nProg: {}\n\n", this);
    //bytecodeRefTable.values().stream().forEach(r -> log.debug(" >$$$ {}", r));
    return bytecodeRefTable.values().stream()
        .filter(BytecodeReference::isUsedInProgram)
        .filter(BytecodeReference::isRecordFieldRef)
        .map(BytecodeReference::getValue)
        .collect(toSet());
  }

  public String toString() {
    return this.getDescriptor();
  }

  public void lexAndParse() {

    if(this.haveLexedAndParsed) { return; }
    this.haveLexedAndParsed = true;

    log.debug("Lexing and parsing: {}", this.getDescriptor());

    try {
      /*
       * Write program text to cache if necessary.
       */
      if(System.getProperty("cacheProgText").equals("true")) {
        BufferedWriter writer = new BufferedWriter(new FileWriter(
           new File("/home/opsdev/ops/cache/" + this.getDescriptor() + ".pc")));
        writer.write(this.programText);
        writer.close();
      }

      InputStream progTextInputStream =
          new ByteArrayInputStream(this.programText.getBytes());
      ANTLRInputStream input = new ANTLRInputStream(progTextInputStream);
      NoErrorTolerancePeopleCodeLexer lexer =
          new NoErrorTolerancePeopleCodeLexer(input);
      this.tokenStream = new CommonTokenStream(lexer);
      PeopleCodeParser parser = new PeopleCodeParser(this.tokenStream);

      parser.removeErrorListeners();
      parser.addErrorListener(new OPSDiagErrorListener());
      parser.getInterpreter()
          .setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
      parser.setErrorHandler(new OPSErrorStrategy());

      this.parseTree = parser.program();

      //log.debug(">>> Parse Tree >>>>>>>>>>>>");
      //log.debug(this.parseTree.toStringTree(parser));
      //log.debug("====================================================");
    } catch(java.io.IOException ioe) {
      throw new OPSVMachRuntimeException(ioe.getMessage());
    }
  }

  public void addFunction(final String name,
      final List<FormalParam> fp, final PTTypeConstraint rTypeConstraint) {
    log.debug("Adding function to table: {}, {}, {}",
        name, fp, rTypeConstraint);

    // IT IS IMPORTANT to use the lowercase version of the function name
    // as the key here; PT allows the caller of a function to use a different
    // combination of letter cases than that defined in the function's signature.
    this.funcTable.put(name.toLowerCase(), new Function(name, fp, rTypeConstraint));
  }

  public Function getFunction(final String funcName) {
    // Remember: keys in table are lower-cased b/c PS does not distinguish
    // b/w function names of differing cases.
    log.debug("Retrieving func {}: {}", funcName,
        this.funcTable.get(funcName.toLowerCase()));
    return this.funcTable.get(funcName.toLowerCase());
  }

  public void registerFunctionImpl(final String funcName,
      final PeopleCodeParser.FuncImplContext parseTreeNode) {

    // IT IS IMPORTANT to use the lowercase version of the function name
    // as the key here; PT allows the caller of a function to use a different
    // combination of letter cases than that defined in the function's signature.
    this.funcImplNodes.put(funcName.toLowerCase(),
        new FuncImpl(funcName, parseTreeNode));
  }

  public FuncImpl getFunctionImpl(final String funcName) {
    // Remember: keys in table are lower-cased b/c PS does not distinguish
    // b/w function names of differing cases.
    return this.funcImplNodes.get(funcName.toLowerCase());
  }

  public boolean hasFunctionImplNamed(final String funcName) {
    // Remember: keys in table are lower-cased b/c PS does not distinguish
    // b/w function names of differing cases.
    return this.funcImplNodes.containsKey(funcName.toLowerCase());
  }

  public void setHasAtLeastOneStatement(final boolean b) {
    this.hasAtLeastOneStatementFlag = b;
  }

  public boolean hasAtLeastOneStatement() {
    // Since this flag is set only when the parse tree is walked,
    // ensure that that has happened before returning the flag.
    this.loadDefnsAndPrograms();
    return this.hasAtLeastOneStatementFlag;
  }

  public void addPathToImportedClass(final String appClassName,
      final AppPackagePath appPkgPath) {
    if (!this.importedAppClasses.containsKey(appClassName)) {
      this.importedAppClasses.put(appClassName, new ArrayList<AppPackagePath>());
    }
    this.importedAppClasses.get(appClassName).add(appPkgPath);
  }

  /*
   * We first search for the class name in the table of app class imports.
   * If no entry exists there, we scan the package imports for a match.
   */
  public AppClassPeopleCodeProg resolveAppClassToProg(final String appClassName) {
    AppPackagePath authoritativePath = null;
    final List<AppPackagePath> pkgList =
        this.importedAppClasses.get(appClassName);
    List<String> appClassParts = null;

    if (pkgList != null) {
      if (pkgList.size() > 1) {
        throw new OPSVMachRuntimeException("Found multiple discrete "
          + "app class imports for an app class; unable to resolve "
          + "authoritative package path.");
      } else {
        authoritativePath = pkgList.get(0);
      }
    } else {
      for (AppPackagePath importedPkgPath
          : this.importedAppPackagePaths) {
        final AppPackage pkg = DefnCache.getAppPackage(
            importedPkgPath.getRootPkgName());
        final Map<String, Void> appClassesInPath =
            pkg.getClassesInPath(importedPkgPath);
        if (appClassesInPath.containsKey(appClassName)) {
          if (authoritativePath == null) {
            authoritativePath = importedPkgPath;
          } else {
            throw new OPSVMachRuntimeException("Found multiple discrete "
              + "app pkg imports for an app class; unable to resolve "
              + "authoritative package path.");
          }
        }
      }
    }

    if (authoritativePath != null) {
      appClassParts = new ArrayList<String>();
      for (String part : authoritativePath.parts) {
        appClassParts.add(part);
      }
      appClassParts.add(appClassName);
    } else {
      throw new OPSVMachRuntimeException("Unable to resolve authoritative "
        + "path to class name (" + appClassName + ").");
    }

    return (AppClassPeopleCodeProg) DefnCache.getProgram(
        new AppClassPeopleCodeProg(appClassParts.toArray(
            new String[appClassParts.size()])));
  }

  public boolean doesImportClass(final String appClassName) {
    return this.importedAppClasses.containsKey(appClassName);
  }
}
