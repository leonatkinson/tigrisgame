/* File: JoinTile.java
 * Description:  object representation for the conflict Join
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

import java.io.Serializable;

public class JoinTile extends Sphere implements Serializable, Cloneable
{
    // the join state for each of the directions next to the join tile.
    // values are either 0 (no king), 1 (first king) or 2 (second king).
    // we could pack this all into one byte, but the join tile is only 
    // one of 176 tiles and only sometimes on he board.
    public static final int NO_KING  = 0;
    public static final int KING_ONE = 1;
    public static final int KING_TWO = 2;

    byte northJoin;
    byte southJoin;
    byte eastJoin;
    byte westJoin;

    // passed in "kingdom numbers", at least two of the four args
    // must have different positive numbers, any with the same positive.
    // numbers are considered joined.
    public JoinTile ( Sphere sp, byte north, byte south, byte east, byte west )
    {
	super( sp );

	// we know there are exactly two adjacent kingdoms,
	// we find one, and if it's the north, set the polarity
	int firstKing = Math.max( Math.max( north, south ),
				  Math.max( east, west ) ); 

	// set the NORTH join var.
	if ( north == firstKing )
	    northJoin = KING_ONE;
	else if ( north > 0 )
	    northJoin = KING_TWO;
	else
	    northJoin = NO_KING;

	// set the SOUTH join var.
	if ( south == firstKing )
	    southJoin = KING_ONE;
	else if ( south > 0 )
	    southJoin = KING_TWO;
	else
	    southJoin = NO_KING;

	// set the EAST join var.
	if ( east == firstKing )
	    eastJoin = KING_ONE;
	else if ( east > 0 )
	    eastJoin = KING_TWO;
	else
	    eastJoin = NO_KING;

	// set the WEST join var.
	if ( west == firstKing )
	    westJoin = KING_ONE;
	else if ( west > 0 )
	    westJoin = KING_TWO;
	else
	    westJoin = NO_KING;
    }

    public Object clone() //throws CloneNotSupportedException
    {
  	try
  	    {
  		return super.clone();
  	    }
  	catch (CloneNotSupportedException e)
  	    {
  		// this shouldn't happen, since we are Cloneable
  		return null;
  	    }
    }

    public boolean isJoined( int dir1, int dir2 )
    {
	byte val1=0, val2=0;
	switch( dir1 ) {
	case Board.NORTH:
	    val1 = northJoin;
	    break;
	case Board.SOUTH:
	    val1 = southJoin;
	    break;
	case Board.EAST:
	    val1 = eastJoin;
	    break;
	case Board.WEST:
	    val1 = westJoin;
	    break;
	}

	switch( dir2 ) {
	case Board.NORTH:
	    val2 = northJoin;
	    break;
	case Board.SOUTH:
	    val2 = southJoin;
	    break;
	case Board.EAST:
	    val2 = eastJoin;
	    break;
	case Board.WEST:
	    val2 = westJoin;
	    break;
	}
	
	return ( val1 != NO_KING && 
		 val1 == val2 );
    }

    public String toString()
    {
	return "Join (" + super.toString() + ")";
    }
}
