/* File: TigrisAnsiColor.java
 * Description:  util class for ansi-colors.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * From http://www.fh-jena.de/~gmueller/Kurs_halle/esc_vt100.html
 *
 * Set Attribute Mode<ESC>[{attr1};...;{attrn}m
 * Sets multiple display attribute settings.
 * The following lists standard attributes:
 * 0 Reset all attributes
 * 1 Bright
 * 2 Dim
 * 4 Underscore
 * 5 Blink
 * 7 Reverse
 * 8 Hidden
 *
 * Foreground Colors
 * 30 Black
 * 31 Red
 * 32 Green
 * 33 Yellow
 * 34 Blue
 * 35 Magenta
 * 36 Cyan
 * 37 White
 *
 * Background Colors
 * 40 Black
 * 41 Red
 * 42 Green
 * 43 Yellow
 * 44 Blue
 * 45 Magenta
 * 46 Cyan
 * 47 White
 */

package com.tigrisgame.textui;

import com.tigrisgame.rules.Sphere;

public class TigrisAnsiColor
{
    // shorthang for set to {black|red|blue|green|cat|normal}
    public static final String TILE  = " . "; 
    public static final String LEAD0 = "(0)";
    public static final String LEAD1 = "(1)";
    public static final String LEAD2 = "(2)";
    public static final String LEAD3 = "(3)";


    public static final String BLACK = "[0;33;40m";
    public static final String RED   = "[0;30;41m";
    public static final String BLUE  = "[0;37;44m";
    public static final String GREEN = "[0;30;42m";

    public static final String RED_FG   = "[0;31m";
    public static final String GREEN_FG = "[0;32m";

    public static final String CAT   = "[0;30;47m" + " X ";
    public static final String WATER = "[0;30;46m" + " ~ ";
    public static final String LAND  = "[0;30;47m" + " . ";

    public static final String HIDDEN_TILE  = CAT + " ? "; 
    public static final String JOIN_TILE  = "[0;30;47m" + " J ";
    public static final String TREASURE = "[0;33;41m";
    public static final String TREASURE_TILE  = TREASURE + " * "; 

    public static final String PLAIN = "[0m";

    public static String getColor( Sphere sp )
    {
	switch ( sp.getColorCode() ) {
	case Sphere.BLACK_NUM:
	    return BLACK;
	case Sphere.RED_NUM:
	    return RED;
	case Sphere.BLUE_NUM:
	    return BLUE;
	case Sphere.GREEN_NUM:
	    return GREEN;
	default:
	    return PLAIN;
	}
    }

    public static String getLeader( int dynasty )
    {
	switch ( dynasty ) {
	case 0:
	    return LEAD0;
	case 1:
	    return LEAD1;
	case 2:
	    return LEAD2;
	case 3:
	    return LEAD3;
	default:
	    System.out.println("No Leader String defined for dynasty " 
			       + dynasty );
	    return null;
	}
    }
}
