package org.ajester.testmodel;

import org.ajester.CodeLocation;

public class BooleanReturn {
	public static final CodeLocation GET_TRUE_LOCATION =
		new CodeLocation(BooleanReturn.class, "getTrue");
	
	public boolean getTrue() {
		return true;
	}
}
