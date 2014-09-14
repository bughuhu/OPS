/*===---------------------------------------------------------------------===*\
|*                       The OpenPplSoft Runtime Project                     *|
|*                                                                           *|
|*              This file is distributed under the MIT License.              *|
|*                         See LICENSE.md for details.                       *|
\*===---------------------------------------------------------------------===*/

package org.openpplsoft.runtime;

import java.math.BigDecimal;
import java.util.*;
import java.lang.reflect.Method;
import org.openpplsoft.types.*;
import org.openpplsoft.runtime.*;
import org.apache.logging.log4j.*;

public class Environment {

  public static PTBoolean TRUE;
  public static PTBoolean FALSE;

  public static Scope globalScope;
  public static Scope componentScope;

  // i.e., XENCSDEV, ENTCSDEV (appears in PS URLs)
  public static String psEnvironmentName;

  private static Map<String, PTPrimitiveType> systemVarTable;
  private static Map<String, Callable> systemFuncTable;

  private static Map<Integer, PTInteger> integerLiteralPool;
  private static Map<String, PTString> stringLiteralPool;
  private static Map<BigDecimal, PTNumber> numberLiteralPool;

  private static Stack<PTType> callStack;

  private static String[] supportedGlobalVars = {"%EmployeeId",
    "%OperatorId", "%Menu", "%Component", "%Action_UpdateDisplay",
    "%Portal", "%Node", "Action_Add"};

  private static Logger log = LogManager.getLogger(Environment.class.getName());

  static {

    // Load static boolean literals.
    TRUE = new PTTypeConstraint<PTBoolean>(PTBoolean.class).alloc();
    TRUE.setReadOnly();
    TRUE.systemWrite(true);

    FALSE = new PTTypeConstraint<PTBoolean>(PTBoolean.class).alloc();
    FALSE.setReadOnly();
    FALSE.systemWrite(false);

    // Setup global and component scopes.
    globalScope = new Scope(Scope.Lvl.GLOBAL);
    componentScope = new Scope(Scope.Lvl.COMPONENT);

    // Create memory pools for supported data types.
    integerLiteralPool = new HashMap<Integer, PTInteger>();
    stringLiteralPool = new HashMap<String, PTString>();
    numberLiteralPool = new HashMap<BigDecimal, PTNumber>();

    // Allocate space for system vars, mark each as read-only.
    systemVarTable = new HashMap<String, PTPrimitiveType>();
    for(String varName : supportedGlobalVars) {
      PTString newStr = new PTTypeConstraint<PTString>(PTString.class).alloc();
      newStr.setReadOnly();
      systemVarTable.put(varName, newStr);
    }

    // Set up constant system variables (these will never change during runtime).
    PTString actionUpdateDisplay =
        new PTTypeConstraint<PTString>(PTString.class).alloc();
    actionUpdateDisplay.write("U");
    actionUpdateDisplay.setReadOnly();
    systemVarTable.put("%Action_UpdateDisplay", actionUpdateDisplay);

    PTString actionAdd =
        new PTTypeConstraint<PTString>(PTString.class).alloc();
    actionAdd.write("A");
    actionAdd.setReadOnly();
    systemVarTable.put("%Action_Add", actionAdd);

    // Set up system variable aliases. TODO: When I have a few of these, create these dynamically.
    systemVarTable.put("%UserId", systemVarTable.get("%OperatorId"));

    // Initialize the call stack.
    callStack = new Stack<PTType>();

    // Cache references to global PT functions to avoid repeated reflection lookups at runtime.
    Method[] methods = GlobalFnLibrary.class.getMethods();
    systemFuncTable = new HashMap<String, Callable>();
    for(Method m : methods) {
      if(m.getName().indexOf("PT_") == 0) {
        systemFuncTable.put(m.getName().substring(3), new Callable(m,
          GlobalFnLibrary.class));
      }
    }
  }

  /**
   * Pushes the provided PT data value to the call stack. If the
   * value is of primitive type, a copy of it will be placed on the call stack,
   * since PT only supports pass-by-reference of objects; primitives are passed
   * by value.
   */
  public static void pushToCallStack(final PTType p) {
    if (p instanceof PTPrimitiveType) {
      PTType copiedPrimitive = p.getOriginatingTypeConstraint().alloc();
      ((PTPrimitiveType) copiedPrimitive).copyValueFrom((PTPrimitiveType) p);
      log.debug("Push\tCallStack\t"
          + (copiedPrimitive == null ? "null" : copiedPrimitive));
      callStack.push(copiedPrimitive);
    } else {
      log.debug("Push\tCallStack\t" + (p == null ? "null" : p));
      callStack.push(p);
    }
  }

  public static PTType popFromCallStack() {
    PTType p = callStack.pop();
    log.debug("Pop\tCallStack\t" + (p == null ? "null" : p));
    return p;
  }

  public static PTType peekAtCallStack() {
    return callStack.peek();
  }

  public static int getCallStackSize() {
    return callStack.size();
  }

  public static void setSystemVar(String var, String value) {
    // Assuming var is mapped to a PTString for now.
    ((PTString)systemVarTable.get(var)).systemWrite(
      Environment.getFromLiteralPool(value).read());
  }

  public static PTPrimitiveType getSystemVar(String var) {

    PTPrimitiveType a = null;
    switch(var) {
      case "%Date":
        a = new PTTypeConstraint<PTDate>(PTDate.class).alloc();
        break;
      default:
        a = systemVarTable.get(var);
    }

    if(a == null) {
      throw new OPSVMachRuntimeException("Attempted to access a system var "
       + "that is undefined: " + var);
    }
    return a;
  }

  public static Callable getSystemFuncPtr(String func) {
    return systemFuncTable.get(func);
  }

  public static PTInteger getFromLiteralPool(Integer val) {
    PTInteger p = integerLiteralPool.get(val);
    if(p == null) {
      p = new PTTypeConstraint<PTInteger>(PTInteger.class).alloc();
      p.setReadOnly();
      p.systemWrite(val);
      integerLiteralPool.put(val, p);
    }
    return p;
  }

  public static PTNumber getFromLiteralPool(BigDecimal val) {
    PTNumber p = numberLiteralPool.get(val);
    if (p == null) {
      p = new PTTypeConstraint<PTNumber>(PTNumber.class).alloc();
      p.setReadOnly();
      p.systemWrite(val);
      numberLiteralPool.put(val, p);
    }
    return p;
  }

  public static PTString getFromLiteralPool(String val) {
    PTString p = stringLiteralPool.get(val);
    if(p == null) {
      p = new PTTypeConstraint<PTString>(PTString.class).alloc();
      p.setReadOnly();
      p.systemWrite(val);
      stringLiteralPool.put(val, p);
    }
    return p;
  }

  public static List<PTType> getDereferencedArgsFromCallStack() {
    List<PTType> args = new ArrayList<PTType>();
    PTType p;
    while(!((p = Environment.peekAtCallStack()) instanceof PTCallFrameBoundary)) {
      PTType arg = Environment.popFromCallStack();
      if (arg instanceof PTReference) {
        arg = ((PTReference) arg).deref();
      }
      args.add(arg);
    }

    // The last argument appears at the top of the stack,
    // so we need to reverse the argument list here before returning it.
    Collections.reverse(args);
    return args;
  }

  public static List<PTType> getArgsFromCallStack() {

    List<PTType> args = new ArrayList<PTType>();
    PTType p;
    while(!((p = Environment.peekAtCallStack()) instanceof PTCallFrameBoundary)) {
      args.add(Environment.popFromCallStack());
    }

    // The last argument appears at the top of the stack,
    // so we need to reverse the argument list here before returning it.
    Collections.reverse(args);
    return args;
  }
}
