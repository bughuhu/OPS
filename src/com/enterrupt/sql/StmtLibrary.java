package com.enterrupt.sql;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.ArrayList;

public class StmtLibrary {

	private static Connection conn;
	private static final String PS_NULL = " ";
	public static ArrayList<ENTStmt> emittedStmts;

	public static void init() {
		emittedStmts = new ArrayList<ENTStmt>();

		try {
			conn = DriverManager.getConnection(
				"jdbc:oracle:oci8:@//10.0.1.88:1521/ENTCSDEV", "SYSADM", "SYSADM");
		} catch(Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}

	public static PreparedStatement getPSPNLGRPDEFN(String b1, String b2) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT DESCR, ACTIONS, VERSION, SEARCHRECNAME, ADDSRCHRECNAME,  SEARCHPNLNAME, LOADLOC, SAVELOC, DISABLESAVE, PRIMARYACTION, DFLTACTION, DFLTSRCHTYPE,  DEFERPROC, EXPENTRYPROC, WSRPCOMPLIANT, REQSECURESSL, INCLNAVIGATION, FORCESEARCH, ALLOWACTMODESEL,  PNLNAVFLAGS, TBARBTNS, SHOWTBAR, ADDLINKMSGSET, ADDLINKMSGNUM, SRCHLINKMSGSET, SRCHLINKMSGNUM,  SRCHTEXTMSGSET, SRCHTEXTMSGNUM, OBJECTOWNERID, TO_CHAR(CAST((LASTUPDDTTM) AS TIMESTAMP),'YYYY-MM-DD-HH24.MI.SS.FF'), LASTUPDOPRID,  DESCRLONG  FROM PSPNLGRPDEFN WHERE PNLGRPNAME = ? AND MARKET = ?");
		stmt.bindVals.put(1, b1);
		stmt.bindVals.put(2, b2);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSPNLGROUP(String b1, String b2) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT PNLNAME, ITEMNAME, HIDDEN, ITEMLABEL, FOLDERTABLABEL, SUBITEMNUM FROM PSPNLGROUP WHERE PNLGRPNAME = ? AND MARKET = ? ORDER BY SUBITEMNUM");
		stmt.bindVals.put(1, b1);
		stmt.bindVals.put(2, b2);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSMENUDEFN(String b1) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT MENULABEL, MENUGROUP, GROUPORDER, MENUORDER, VERSION, INSTALLED, GROUPSEP, MENUSEP, MENUTYPE, OBJECTOWNERID, LASTUPDOPRID, TO_CHAR(CAST((LASTUPDDTTM) AS TIMESTAMP),'YYYY-MM-DD-HH24.MI.SS.FF'), DESCR, DESCRLONG FROM PSMENUDEFN WHERE MENUNAME = ?");
		stmt.bindVals.put(1, b1);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSMENUITEM(String b1) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT BARNAME, ITEMNAME, BARLABEL, ITEMLABEL, MARKET, ITEMTYPE, PNLGRPNAME, SEARCHRECNAME, ITEMNUM, XFERCOUNT FROM PSMENUITEM WHERE MENUNAME = ? ORDER BY ITEMNUM");
		stmt.bindVals.put(1, b1);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSRECDEFN(String b1) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT VERSION, FIELDCOUNT, RECTYPE, RECUSE, OPTTRIGFLAG, AUDITRECNAME, SETCNTRLFLD, RELLANGRECNAME, OPTDELRECNAME, PARENTRECNAME, QRYSECRECNAME, SQLTABLENAME, BUILDSEQNO, OBJECTOWNERID, TO_CHAR(CAST((LASTUPDDTTM) AS TIMESTAMP),'YYYY-MM-DD-HH24.MI.SS.FF'), LASTUPDOPRID, SYSTEMIDFIELDNAME, TIMESTAMPFIELDNAME, RECDESCR, AUXFLAGMASK, DESCRLONG  FROM PSRECDEFN WHERE RECNAME = ?");
		stmt.bindVals.put(1, b1);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSDBFIELD_PSRECFIELD_JOIN(String b1) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT VERSION, A.FIELDNAME, FIELDTYPE, LENGTH, DECIMALPOS, FORMAT, FORMATLENGTH, IMAGE_FMT, FORMATFAMILY, DISPFMTNAME, DEFCNTRYYR,IMEMODE,KBLAYOUT,OBJECTOWNERID, DEFRECNAME, DEFFIELDNAME, CURCTLFIELDNAME, USEEDIT, USEEDIT2, EDITTABLE, DEFGUICONTROL, SETCNTRLFLD, LABEL_ID, TIMEZONEUSE, TIMEZONEFIELDNAME, CURRCTLUSE, RELTMDTFIELDNAME, TO_CHAR(CAST((B.LASTUPDDTTM) AS TIMESTAMP),'YYYY-MM-DD-HH24.MI.SS.FF'), B.LASTUPDOPRID, B.FIELDNUM, A.FLDNOTUSED, A.AUXFLAGMASK, B.RECNAME FROM PSDBFIELD A, PSRECFIELD B WHERE B.RECNAME = ? AND A.FIELDNAME = B.FIELDNAME AND B.SUBRECORD = 'N' ORDER BY B.RECNAME, B.FIELDNUM");
		stmt.bindVals.put(1, b1);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSDBFLDLBL(String b1) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT FIELDNAME, LABEL_ID, LONGNAME, SHORTNAME, DEFAULT_LABEL FROM PSDBFLDLABL WHERE FIELDNAME IN (SELECT A.FIELDNAME FROM PSDBFIELD A, PSRECFIELD B WHERE B.RECNAME = ? AND A.FIELDNAME = B.FIELDNAME) ORDER BY FIELDNAME, LABEL_ID");
		stmt.bindVals.put(1, b1);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSPCMPROG_CompPCList(String b1, String b2, String b3, String b4) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT OBJECTID1,OBJECTVALUE1, OBJECTID2,OBJECTVALUE2, OBJECTID3,OBJECTVALUE3, OBJECTID4,OBJECTVALUE4, OBJECTID5,OBJECTVALUE5, OBJECTID6,OBJECTVALUE6, OBJECTID7,OBJECTVALUE7  FROM PSPCMPROG WHERE OBJECTID1 = ? AND OBJECTVALUE1 = ? AND  OBJECTID2 = ? AND OBJECTVALUE2 = ? ORDER BY OBJECTID1,OBJECTVALUE1, OBJECTID2,OBJECTVALUE2, OBJECTID3,OBJECTVALUE3, OBJECTID4,OBJECTVALUE4, OBJECTID5,OBJECTVALUE5, OBJECTID6,OBJECTVALUE6, OBJECTID7,OBJECTVALUE7");
		stmt.bindVals.put(1, b1);
		stmt.bindVals.put(2, b2);
		stmt.bindVals.put(3, b3);
		stmt.bindVals.put(4, b4);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSPCMPROG_SearchRecordPCList(String b1, String b2) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT OBJECTID1,OBJECTVALUE1, OBJECTID2,OBJECTVALUE2, OBJECTID3,OBJECTVALUE3, OBJECTID4,OBJECTVALUE4, OBJECTID5,OBJECTVALUE5, OBJECTID6,OBJECTVALUE6, OBJECTID7,OBJECTVALUE7  FROM PSPCMPROG WHERE OBJECTID1 = ? AND OBJECTVALUE1 = ? ORDER BY OBJECTID1,OBJECTVALUE1, OBJECTID2,OBJECTVALUE2, OBJECTID3,OBJECTVALUE3, OBJECTID4,OBJECTVALUE4, OBJECTID5,OBJECTVALUE5, OBJECTID6,OBJECTVALUE6, OBJECTID7,OBJECTVALUE7");
		stmt.bindVals.put(1, b1);
		stmt.bindVals.put(2, b2);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSPCMPROG_GetPROGTXT(String b1, String b2, String b3, String b4, String b5, String b6, String b7, String b8, String b9, String b10, String b11, String b12, String b13, String b14) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT VERSION, PROGRUNLOC, NAMECOUNT, PROGLEN, PROGTXT, LICENSE_CODE, TO_CHAR(CAST((LASTUPDDTTM) AS TIMESTAMP),'YYYY-MM-DD-HH24.MI.SS.FF'), LASTUPDOPRID, PROGFLAGS, PROGEXTENDS, PROGSEQ FROM PSPCMPROG WHERE  OBJECTID1 = ? AND OBJECTVALUE1 = ? AND  OBJECTID2 = ? AND OBJECTVALUE2 = ? AND  OBJECTID3 = ? AND OBJECTVALUE3 = ? AND  OBJECTID4 = ? AND OBJECTVALUE4 = ? AND  OBJECTID5 = ? AND OBJECTVALUE5 = ? AND  OBJECTID6 = ? AND OBJECTVALUE6 = ? AND  OBJECTID7 = ? AND OBJECTVALUE7 = ? ORDER BY PROGSEQ");
		stmt.bindVals.put(1, b1);
		stmt.bindVals.put(2, b2);
		stmt.bindVals.put(3, b3);
		stmt.bindVals.put(4, b4);
		stmt.bindVals.put(5, b5);
		stmt.bindVals.put(6, b6);
		stmt.bindVals.put(7, b7);
		stmt.bindVals.put(8, b8);
		stmt.bindVals.put(9, b9);
		stmt.bindVals.put(10, b10);
		stmt.bindVals.put(11, b11);
		stmt.bindVals.put(12, b12);
		stmt.bindVals.put(13, b13);
		stmt.bindVals.put(14, b14);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSPCMPROG_GetRefs(String b1, String b2, String b3, String b4, String b5, String b6, String b7, String b8, String b9, String b10, String b11, String b12, String b13, String b14) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT RECNAME, REFNAME, PACKAGEROOT, QUALIFYPATH, NAMENUM FROM PSPCMNAME WHERE  OBJECTID1 = ? AND OBJECTVALUE1 = ? AND  OBJECTID2 = ? AND OBJECTVALUE2 = ? AND  OBJECTID3 = ? AND OBJECTVALUE3 = ? AND  OBJECTID4 = ? AND OBJECTVALUE4 = ? AND  OBJECTID5 = ? AND OBJECTVALUE5 = ? AND  OBJECTID6 = ? AND OBJECTVALUE6 = ? AND  OBJECTID7 = ? AND OBJECTVALUE7 = ? ORDER BY NAMENUM");
		stmt.bindVals.put(1, b1);
		stmt.bindVals.put(2, b2);
		stmt.bindVals.put(3, b3);
		stmt.bindVals.put(4, b4);
		stmt.bindVals.put(5, b5);
		stmt.bindVals.put(6, b6);
		stmt.bindVals.put(7, b7);
		stmt.bindVals.put(8, b8);
		stmt.bindVals.put(9, b9);
		stmt.bindVals.put(10, b10);
		stmt.bindVals.put(11, b11);
		stmt.bindVals.put(12, b12);
		stmt.bindVals.put(13, b13);
		stmt.bindVals.put(14, b14);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSPNLDEFN(String b1) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT VERSION, PNLTYPE, GRIDHORZ, GRIDVERT, FIELDCOUNT, MAXPNLFLDID, HELPCONTEXTNUM, PANELLEFT, PANELTOP, PANELRIGHT, PANELBOTTOM, PNLSTYLE, STYLESHEETNAME, PNLUSE, DEFERPROC, DESCR, POPUPMENU, LICENSE_CODE, TO_CHAR(CAST((LASTUPDDTTM) AS TIMESTAMP),'YYYY-MM-DD-HH24.MI.SS.FF'), LASTUPDOPRID, OBJECTOWNERID, DESCRLONG FROM PSPNLDEFN WHERE PNLNAME = ?");
		stmt.bindVals.put(1, b1);
		return stmt.generatePreparedStmt(conn);
	}

	public static PreparedStatement getPSPNLFIELD(String b1) throws Exception {
		ENTStmt stmt = new ENTStmt("SELECT PNLFLDID, FIELDTYPE, EDITSIZE, FIELDLEFT, FIELDTOP, FIELDRIGHT, FIELDBOTTOM, EDITLBLLEFT, EDITLBLTOP, EDITLBLRIGHT, EDITLBLBOTTOM, DSPLFORMAT, DSPLFILL, LBLTYPE, LBLLOC, LBLPADSIZE, LABEL_ID, LBLTEXT, FIELDUSE, FIELDUSETMP, DEFERPROC, OCCURSLEVEL, OCCURSCOUNT1, OCCURSCOUNT2, OCCURSCOUNT3, OCCURSOFFSET1, OCCURSOFFSET2, OCCURSOFFSET3, PNLFIELDNAME, RECNAME, FIELDNAME, SUBPNLNAME, ONVALUE, OFFVALUE, ASSOCFIELDNUM, FIELDSTYLE, LABELSTYLE, FIELDSIZETYPE, LABELSIZETYPE, PRCSNAME, PRCSTYPE, FORMATFAMILY, DISPFMTNAME, PROMPTFIELD, POPUPMENU, TREECTRLID, TREECTRLTYPE, MULTIRECTREE, NODECOUNT, GRDCOLUMNCOUNT, GRDSHOWCOLHDG, GRDSHOWROWHDG, GRDODDROWSTYLE, GRDEVENROWSTYLE, GRDACTIVETABSTYLE, GRDINACTIVETABSTYL, GRDNAVBARSTYLE, GRDLABELSTYLE, GRDLBLMSGSET, GRDLBLMSGNUM, GRDLBLALIGN, GRDACTTYPE, TABENABLE, PBDISPLAYTYPE, OPENNEWWINDOW, URLDYNAMIC,  URL_ID, GOTOPORTALNAME, GOTONODENAME, GOTOMENUNAME, GOTOPNLGRPNAME,  GOTOMKTNAME, GOTOPNLNAME, GOTOPNLACTION, SRCHBYPNLDATA, SCROLLACTION, TOOLACTION, CONTNAME, CONTNAMEOVER, CONTNAMEDISABLE, PTLBLIMGCOLLAPSE, PTLBLIMGEXPAND, SELINDICATORTYPE, PTADJHIDDENFIELDS, PTCOLLAPSEDATAAREA, PTDFLTVIEWEXPANDED, PTHIDEFIELDS, SHOWCOLHIDEROWS, PTLEBEXPANDFIELD, SHOWTABCNTLBTN, SECUREINVISIBLE, ENABLEASANCHOR, URLENCODEDBYAPP, USEDEFAULTLABEL, GRDALLOWCOLSORT FROM PSPNLFIELD WHERE PNLNAME = ? ORDER BY FIELDNUM");
		stmt.bindVals.put(1, b1);
		return stmt.generatePreparedStmt(conn);
	}



	/**
	 * NOTE: This is not a statement executed by PeopleTools; this is used by Enterrupt
	 * to verify that the internal PeopleCode parser produced the correct program text.
	 * It must not appear in the emitted SQL statements list.
	 */
	public static PreparedStatement getPSPCMTXT(String b1, String b2, String b3, String b4, String b5, String b6, String b7, String b8, String b9, String b10, String b11, String b12, String b13, String b14) throws Exception {
		PreparedStatement pstmt = conn.prepareStatement("SELECT PCTEXT FROM PSPCMTXT WHERE  OBJECTID1 = ? AND OBJECTVALUE1 = ? AND  OBJECTID2 = ? AND OBJECTVALUE2 = ? AND  OBJECTID3 = ? AND OBJECTVALUE3 = ? AND  OBJECTID4 = ? AND OBJECTVALUE4 = ? AND  OBJECTID5 = ? AND OBJECTVALUE5 = ? AND  OBJECTID6 = ? AND OBJECTVALUE6 = ? AND  OBJECTID7 = ? AND OBJECTVALUE7 = ? ORDER BY PROGSEQ");
		pstmt.setString(1, b1);
		pstmt.setString(2, b2);
		pstmt.setString(3, b3);
		pstmt.setString(4, b4);
		pstmt.setString(5, b5);
		pstmt.setString(6, b6);
		pstmt.setString(7, b7);
		pstmt.setString(8, b8);
		pstmt.setString(9, b9);
		pstmt.setString(10, b10);
		pstmt.setString(11, b11);
		pstmt.setString(12, b12);
		pstmt.setString(13, b13);
		pstmt.setString(14, b14);
		return pstmt;
	}

	public static void disconnect() {
		try {
			conn.close();
		} catch(Exception ex) {
			ex.printStackTrace();
			System.exit(1);
		}
	}
}