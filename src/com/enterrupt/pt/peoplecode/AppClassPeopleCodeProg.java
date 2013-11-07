package com.enterrupt.pt.peoplecode;

import java.lang.StringBuilder;
import com.enterrupt.parser.*;
import com.enterrupt.runtime.*;
import com.enterrupt.pt.*;

public class AppClassPeopleCodeProg extends PeopleCodeProg {

	public String[] pathParts;
	public AppPackage rootPackage;

	public AppClassPeopleCodeProg(String[] path) {
		super();
		this.pathParts = path;
		this.event = "OnExecute";
		this.initBindVals();
		this.rootPackage = DefnCache.getAppPackage(this.bindVals[1]);
	}

	protected void initBindVals() {

        /**
         * Due to the variable length nature of app class paths,
         * we need to determine which bind values we'll be querying the database
         * with based on the length provided and knowledge of OBJECTID constants.
         */
        int pathPartIdx = 0;
        int nextObjectId = 105; // 105 through 107 == App Class
        this.bindVals = new String[14];
        for(int i = 0; i < this.bindVals.length; i+=2) {
            if(i == 0) {
                this.bindVals[0] = "104"; // 104 == App Package
                this.bindVals[1] = pathParts[pathPartIdx++];
            } else if(pathPartIdx == (pathParts.length - 1)) {
                this.bindVals[i] = "107"; // 107 == final App Class in hierarchy
                this.bindVals[i+1] = pathParts[pathPartIdx++];
            } else if(pathPartIdx < pathParts.length) {
                this.bindVals[i] = "" + nextObjectId++;
                this.bindVals[i+1] = pathParts[pathPartIdx++];
            } else {

                // The event OBJECTID/OBJECTVAL pair must follow the final app class.
                if(this.bindVals[i-2].equals("107")) {
                    this.bindVals[i] = "12";      // 12 == Event
                    this.bindVals[i+1] = this.event;
                } else {
                    this.bindVals[i] = "0";
                    this.bindVals[i+1] = PSDefn.NULL;
                }
            }
        }
	}

	public String getDescriptor() {

		StringBuilder sb = new StringBuilder();
		sb.append("AppPackagePC.");
		for(int i = 0; i < this.pathParts.length; i++) {
			sb.append(this.pathParts[i]).append(".");
		}
		sb.append(this.event);
		return sb.toString();
	}

	protected void subclassTokenHandler(Token t, PeopleCodeTokenStream stream,
		int recursionLevel, LFlag lflag) {

		/**
		 * Detect application classes referenced as instance and property values in an object
		 * definition.
		 */
		if(t.flags.contains(TFlag.INSTANCE) || t.flags.contains(TFlag.PROPERTY)) {

			t = stream.readNextToken();
			Token l = stream.peekNextToken();

			if(t.flags.contains(TFlag.PURE_STRING)
				&& l.flags.contains(TFlag.COLON)
				&& importedAppPackages.get(t.pureStrVal) != null) {

				String[] path = this.getAppClassPathFromStream(t, stream);

				PeopleCodeProg prog = new AppClassPeopleCodeProg(path);
				prog = DefnCache.getProgram(prog);

				referencedProgs.add(prog);

				// Load the program's initial metadata.
				prog.init();

				/**
				 * I added this call in order to align ENT with the Component PC loading section of the
				 * the trace file; I've learned that when PT encounters a referenced object in an app
				 * class, it recursively loads that app class's references immediately. Thus I added this call.
			     * It's possible that this call may need to be locked down to LFlag.COMPONENT
				 * if issues with Record PC loading surface later on. At this time, running
				 * this unconditionally is not interfering with the previous sections of the trace file.
				 * TODO: keep this in mind.
				 */
				prog.recurseLoadDefnsAndPrograms(recursionLevel+1, lflag);
			}
		}
	}
}