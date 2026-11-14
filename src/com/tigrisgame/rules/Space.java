/* File: Space.java
 * Description:  Interface for a space on the Board.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

public interface Space extends Location
{
    // basic properties that are checked
    // in game for move legality and in gui
    // for painting.
    boolean isEmpty(); //all "has" methods below false.
    boolean isWater();
    boolean isLand();

    // boolean checks for properties of the space.
    // quicker than getting the property and checking
    // for null in some implementations.
    boolean hasTreasure();
    boolean hasMustTakeTreasure();
    boolean hasTile();
    boolean hasLeader();
    boolean hasMonument();
    boolean hasCatastrophe();
    boolean hasJoinTile();

    // returns (and possibly constructs) objects
    // representing the contents of the space.
    Leader getLeader();
    Tile getTile();
    Monument getMonument();

    // toChar may be printed out in an ascii map and toString
    // used for a more verbose debugging or display.
    char toChar();
    String toString();
    
    // PRIVATE - mutators must be done through the board.
    // however, spaces must implement these functions to work properly.
    // void putTile( Sphere s );
    // void putLeader( Sphere s, int dynasty );
    // void putMonument( Sphere s, Sphere s );
    // void putCatastrophe();
    // void takeTreasure();
}
