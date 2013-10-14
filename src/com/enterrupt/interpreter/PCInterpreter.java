package com.enterrupt.interpreter;

import com.enterrupt.pt_objects.PeopleCodeProg;
import java.util.HashMap;

public class PCInterpreter {

	private static boolean hasInitialized = false;
	private static ElementParser[] allParsers;
	private static HashMap<Byte, ElementParser> parserTable;

	public static void interpret(PeopleCodeProg prog) throws Exception {

		init();
		prog.setByteCursorPos(37); 	// Program begins at byte 37.
		boolean endDetected = false,
				firstLine = true,
				in_declare = false,
				startOfLine = true,
				and_indicator = false,
				did_newline = false;
		ElementParser lastParser = null;
		int nIndent = 0;

		/*int i = 0;
		for(byte b : prog.progBytes) {
			System.out.printf("%d: 0x%02X\n", i, b);
			i++;
		}*/

		while(prog.byteCursorPos < prog.progBytes.length && !endDetected) {
			if(endDetected = (prog.getCurrentByte() == (byte) 7)) {
				break;
			}
			byte b = prog.readNextByte();
			System.out.printf("Getting parser for byte: 0x%02X\n", b);
			ElementParser p = parserTable.get(new Byte(b));
			if(p == null) {
				System.out.println("[ERROR] Reached unimplementable byte.");
				break;
			} else {
				/* TODO: Fill out as needed. */
				in_declare = (in_declare &&
					!((lastParser != null && (lastParser.format & PCToken.NEWLINE_AFTER) > 0)
										  || (lastParser.format == PCToken.SEMICOLON)));

				if(!firstLine
					&& p.format != PCToken.PUNCTUATION
					&& (p.format & PCToken.SEMICOLON) == 0
					&& !in_declare
					&& (	(	(lastParser != null && ((lastParser.format & PCToken.NEWLINE_AFTER) > 0)
									|| (lastParser.format == PCToken.SEMICOLON))
					  		&&  (p.format & PCToken.COMMENT_ON_SAME_LINE) == 0)
						|| ((p.format & PCToken.NEWLINE_BEFORE) > 0))
						|| ((p.format & PCToken.NEWLINE_ONCE) > 0 && !did_newline && prog.readAhead() != (byte) 21)) {

					prog.appendProgText('\n');
					startOfLine = true;
					did_newline = true;
				}

				if(startOfLine && p.writesNonBlank()) {
					for(int i=0; i < nIndent + (and_indicator? 2 : 0); i++) {
						prog.appendProgText("   ");
					}
				}
			}
			firstLine = false;
			int initialByteCursorPos = prog.byteCursorPos;
			p.parse(prog);
			in_declare = in_declare || (p.format & PCToken.IN_DECLARE) > 0;
			startOfLine = startOfLine && !p.writesNonBlank();
			did_newline = did_newline && (prog.byteCursorPos == initialByteCursorPos);
			and_indicator = (p.format & PCToken.AND_INDICATOR) > 0
				|| (and_indicator && (p.format & PCToken.COMMENT_ON_SAME_LINE) != 0);
			lastParser = p;
			if((p.format & PCToken.RESET_INDENT_AFTER) > 0) {
				nIndent = 0;
			}
		}
		System.out.println(prog.getProgText());	
	}

	public static void init() {

		if(hasInitialized) return;

		// Array of all available parsers.
		allParsers = new ElementParser[] {
			new SimpleElementParser((byte) 28, "If", PCToken.IF_STYLE),		// 0x1C
			new CommentParser((byte) 36, PCToken.NEWLINE_BEFORE_AND_AFTER) 	// 0x24
		};

		// Initialize hash table of parsers, indexed by start byte.
		parserTable = new HashMap<Byte, ElementParser>();
		for(ElementParser p : allParsers) {
			parserTable.put(new Byte(p.getStartByte()), p);
		}

		hasInitialized = true;
	}
}
