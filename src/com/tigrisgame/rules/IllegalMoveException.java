/* File: IllegalMoveException.java
 * Description:  Exception object for illegal moves.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

public class IllegalMoveException extends Exception
{
    public static final int LEAD_JOIN_KING          = 101;
    public static final int LEAD_OVER_RIVER         = 102;
    public static final int LEAD_OVER_FULL          = 103;
    public static final int LEAD_NEXT_TEMPLE        = 104;

    public static final int TILE_JOIN_THREE         = 201;
    public static final int TILE_BLUE_OVER_LAND     = 202;
    public static final int TILE_OTHER_OVER_RIVER   = 203;
    public static final int TILE_OVER_FULL          = 204;

    public static final int CAT_OVER_LEAD           = 301;
    public static final int CAT_OVER_MON            = 302;
    public static final int CAT_OVER_TREAS          = 303;
    public static final int CAT_OVER_CAT            = 304;

    public static final int SUPPORT_WRONG_COLOR     = 401;
    public static final int SUPPORT_NO_DUMPING      = 402;

    public static final int MON_WRONG_COLOR         = 501;
    public static final int MON_INCLUDE_LAST        = 502;
    public static final int MON_TILE_SQUARE         = 503;

    public static final int TREAS_CORNER            = 601;

    public static final int SPHERE_NOT_CONFLICT     = 701;

    int code = 0;

    public IllegalMoveException() {}

    public IllegalMoveException( String error )
    {
	super( error );
    }

    public IllegalMoveException( int code )
    {
        super( "" + code );
	this.code = code;
    }

    public int getCode()
    {
	return code;
    }

   public String toString()
   {
      return "IllegalMoveException: " + getMessage() + " " + code;
   }
}
