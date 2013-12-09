package com.enterrupt.antlr4;

import java.util.*;
import java.lang.reflect.*;
import com.enterrupt.types.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import com.enterrupt.interpreter.*;
import com.enterrupt.runtime.*;
import org.apache.logging.log4j.*;
import com.enterrupt.pt.peoplecode.*;
import com.enterrupt.antlr4.frontend.*;

public class ProgLoaderVisitor extends PeopleCodeBaseVisitor<Void> {

	private PeopleCodeProg srcProg;

	private static Logger log = LogManager.getLogger(ProgLoaderVisitor.class.getName());

	public ProgLoaderVisitor(PeopleCodeProg srcProg) {
		this.srcProg = srcProg;
	}

	/**
	 * When an app package is imported, load the package's definition and note
	 * that it was imported.
	 */
	public Void visitAppClassImport(PeopleCodeParser.AppClassImportContext ctx) {
		String appPkgName = ctx.appClassPath().GENERIC_ID(0).getText();
		DefnCache.getAppPackage(appPkgName);
		this.srcProg.importedAppPackages.put(appPkgName, true);
		return null;
	}

	/**
	 * When a variable declaration in a non-app class program references
	 * an object in an app class, we need to parse out the app class reference and
	 * load that program's OnExecute program.
	 */
	public Void visitVarDeclaration(PeopleCodeParser.VarDeclarationContext ctx) {

		/**
		 * Only take action when source program is classic (not app class).
		 * TODO: This will need to change when I reorganize the peoplecode package.
		 */
		if(!(this.srcProg instanceof ClassicPeopleCodeProg)) {
			return null;
		}

		if(ctx.varType().appClassPath() != null) {
			ArrayList<String> appClassParts = new ArrayList<String>();
			for(TerminalNode id : ctx.varType().appClassPath().GENERIC_ID()) {
				appClassParts.add(id.getText());
			}
			PeopleCodeProg prog = new AppClassPeopleCodeProg(appClassParts.toArray(
				new String[appClassParts.size()]));
			prog = DefnCache.getProgram(prog);
			this.srcProg.referencedProgs.add(prog);

			// Load the referenced program's initial metadata.
			prog.init();
		}

		return null;
	}

	/**
	 * Detect references to externally-defined functions. We make note of the
	 * referenced program at this time; later, once we've counted any and all
	 * function calls to this and other functions defined in the referenced program,
	 * that program will itself be loaded, provided that the number of references is > 0.
	 */
	public Void visitExtFuncImport(PeopleCodeParser.ExtFuncImportContext ctx) {

		/**
		 * Only take action when source program is classic (not app class).
		 * TODO: This will need to change when I reorganize the peoplecode package.
		 */
		if(!(this.srcProg instanceof ClassicPeopleCodeProg)) {
			return null;
		}

		String fnName = ctx.GENERIC_ID().getText();
		String recname = ctx.recDefnPath().GENERIC_ID(0).getText();
		String fldname = ctx.recDefnPath().GENERIC_ID(1).getText();

		// Load the record defn it it hasn't already been cached.
		DefnCache.getRecord(recname);

		PeopleCodeProg prog = new RecordPeopleCodeProg(recname, fldname,
			ctx.event().getText());
		prog = DefnCache.getProgram(prog);

		this.srcProg.referencedProgs.add(prog);
		this.srcProg.recordProgFnCalls.put(fnName, (RecordPeopleCodeProg)prog);

		// Load the prog's initial metadata if it hasn't already been cached.
		prog.init();

		return null;
	}

	/**
	 * Detect function (*not* method) calls; if a call corresponds to a
	 * function referenced in a previously seen "Declare" stmt, mark that program
	 * as having at least one call to it. Calls to index into a rowset using
	 * PeopleCode's "(<int>)" syntax should be ignored here.
	 */
	public Void visitExprFnOrRowsetCall(PeopleCodeParser.ExprFnOrRowsetCallContext ctx) {
		visit(ctx.expr());

		PeopleCodeParser.IdContext id = null;

		if(ctx.expr() instanceof PeopleCodeParser.ExprIdContext) {
			id = ((PeopleCodeParser.ExprIdContext)ctx.expr()).id();
		} else if(ctx.expr() instanceof PeopleCodeParser.ExprMethodOrStaticRefContext) {
			/**
		     * If a method call is the expr on which this fn/rowset call operates on,
			 * i.e., "&arr.Push(%Menu)", we can ignore it; methods cannot be the subject
			 * of "Declare" stmts.
			 */
			return null;
		} else if(ctx.expr() instanceof PeopleCodeParser.ExprFnOrRowsetCallContext) {
			/**
			 * If a function call is the expr on which this fn/rowset call operates on,
			 * i.e., "GetRowset(Scroll.ENRL_REQ_DETAIL)(1)", we can ignore it;
			 * the name of the function was already processed by the explicit call to
			 * visit(ctx.expr()) above.
			 */
			return null;
		} else {
			throw new EntVMachRuntimeException("Encountered unexpected expression type "+
				"preceding a function call or rowset index call: " + ctx.expr().getText());
		}

		/**
		 * Only GENERIC_IDs are considered to be identifiers representative of
		 * a function name; if the identifier is a variable, it is
		 * part of a rowset call (i.e, "&rs(1)") and should not be checked.
		 */
		if(id.GENERIC_ID() != null) {

			RecordPeopleCodeProg referencedProg = this.srcProg.recordProgFnCalls
				.get(id.GENERIC_ID().getText());

			/**
			 * If the referencedProg is null, the referenced function has a scope
			 * beyond the program (is a system function). Otherwise, the function
			 * is in program scope, and is thus defined in the referencedProg;
			 * we must note that a reference to that program exists to ensure that
			 * we load that program later.
			 */
			if(referencedProg != null) {
				srcProg.confirmedRecordProgCalls.put(referencedProg, true);
			}
		}

		return null;
	}
}
