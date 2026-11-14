/* File: Sphere.java
 * Description:   * an immutable "Enumeration" of Sphere colors.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

import java.io.Serializable;

public class Sphere implements Serializable
{
    // legal values must be 0 to 3 so they can
    // fit in 2 bytes in some implemenations.
    // package private on purpose
    public static final int
       INVALID    = -1,
       // the standard spheres.
       BLACK_NUM  = 0,
       RED_NUM    = 1,
       BLUE_NUM   = 2,
       GREEN_NUM  = 3,
       // sphere for the expansion.
       YELLOW_NUM = 4,
       // grey is a special sphere used for graphic purposes.
       GREY_NUM   = 5,
       // hidden is to signify an object with unknown or hidden sphere.
       HIDDEN_NUM = 6;

    public static final char 
       BLACK_CHAR = 'B',
       RED_CHAR   = 'R',
       BLUE_CHAR  = 'U',
       GREEN_CHAR = 'G',
       YELLOW_CHAR= 'Y',
       GREY_CHAR  = 'E',
       HIDDEN_CHAR= 'H';


    public static final Sphere
       BLACK = new Sphere( BLACK_NUM ),
       RED   = new Sphere( RED_NUM ),
       BLUE  = new Sphere( BLUE_NUM ),
       GREEN = new Sphere( GREEN_NUM ),
       YELLOW= new Sphere( YELLOW_NUM ),
       GREY  = new Sphere( GREY_NUM ),
       HIDDEN= new Sphere( HIDDEN_NUM );

    protected int color;

    public Sphere()
    {
	color = INVALID;
    }

    public Sphere(int color)
    {
	this.color = color;
    }

    public Sphere( Sphere sphere )
    {
  	if ( sphere != null )
  	    this.color = sphere.color;
  	else
  	    this.color = INVALID;
    }

    // color code is also assumed to be the index in the getSpheres() array.
    public int getColorCode()
    {
	return color;
    }
    
    public boolean isRed()
    {
	return (color == RED_NUM);
    }
    public boolean isGreen()
    {
	return (color == GREEN_NUM);
    }

    public boolean isBlue()
    {
	return (color == BLUE_NUM);
    }    

    public boolean isBlack()
    {
	return (color == BLACK_NUM);
    }

    public boolean isYellow()
    {
	return (color == YELLOW_NUM);
    }

    public boolean isGrey()
    {
	return (color == GREY_NUM);
    }

    public boolean isHidden()
    {
	return color == HIDDEN_NUM;
    }

    public boolean isInvalid()
    {
	return (color ==INVALID);
    }

    public boolean isSphereEqual( Sphere sp )
    {
	return ( sp != null &&
		 this.color == sp.color);
    }

    public boolean equals( Object obj )
    {
	return ( obj instanceof Sphere &&
		 isSphereEqual( (Sphere) obj ));
    }

    public static Sphere [] getSpheres()
    {
	return new Sphere [] { BLACK,
			       RED,
			       BLUE,
			       GREEN };
    }

    public static Sphere getSphereFromChar( int sphereChar ) 
    {
	Sphere sphere = null;
	switch (sphereChar) {
	case 'b':
	case 'B':
	    sphere = BLACK;
	    break;
	case 'r':
	case 'R':
	case '*':
	case '+':
	    sphere =  RED;
	    break;
	case 'u':
	case 'U':
	    sphere =  BLUE;
	    break;
	case 'g':
	case 'G':
	    sphere =  GREEN;
	    break;
	case 'y':
	case 'Y':
	    sphere =  YELLOW;
	    break;
	case 'e':
	case 'E':
	    sphere =  GREY;
	    break;
	case 'h':
	case 'H':
	case '?':
	    sphere =  HIDDEN;
	    break;
	default: 
	}
	return sphere;
    }

    public char toChar()
    {
	switch ( color )
	    {
	    case BLACK_NUM:
		return BLACK_CHAR;
	    case RED_NUM:
		return RED_CHAR;
	    case BLUE_NUM:
		return BLUE_CHAR;
	    case GREEN_NUM:
		return GREEN_CHAR;
	    case YELLOW_NUM:
		return YELLOW_CHAR;
	    case GREY_NUM:
		return GREY_CHAR;
	    case HIDDEN_NUM:
		return HIDDEN_CHAR;
	    case INVALID:
	    default:
		return '?';
	    }
    }

    public String toString()
    {
	switch ( color )
	    {
	    case BLACK_NUM:
		return "Black";
	    case RED_NUM:
		return "Red";
	    case BLUE_NUM:
		return "Blue";
	    case GREEN_NUM:
		return "Green";
	    case YELLOW_NUM:
		return "Yellow";
	    case GREY_NUM:
		return "Grey";
	    case HIDDEN_NUM:
		return "Hidden";
	    case INVALID:
	    default:
		return "Invalid";
	    }
    }
}
