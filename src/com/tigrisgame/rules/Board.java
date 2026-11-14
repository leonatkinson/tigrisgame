/* File: Board.java
 * Description:  Game state for E&T Board (interface)
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */
 
package com.tigrisgame.rules;

public interface Board extends TileHolder
{
    public static final int NORTH = 1;
    public static final int SOUTH = 2;
    public static final int EAST  = 3;
    public static final int WEST  = 4;
    

    // YES
    // general utility methods.
    int getNumRows();
    int getNumCols();
    void printAsciiMap();
    void printKingdoms();
    Board deepCopy();
    void init( Board board );

    // mutator methods so that board controls the internal
    // representation of the Spaces and Kingdoms
    // putMonument modifies 4 spaces with loc as upper left.
    // empty space makes the space empty land or water.
    void putTile( Location loc, Sphere s );
    void putLeader( Location loc, Sphere s, int dynasty );
    void putMonument( Location loc, Monument mon ); 
    void putCatastrophe( Location loc );    

    // puts a join tile in the named location.
    // the board will infer proper connectivity
    // from adjacent spaces.
    void putJoinTile( Location loc, Sphere s );
    // the join tile is stored in the board object.
    // a space with the join tile will come across as empty.
    JoinTile getJoinTile();

    void takeTreasure( Location loc );
    void emptyLocation( Location loc );

    // update the kingdoms either
    // () from scratch OR
    // ( Space ) given one space has changed.
    // ( Kingdom ) given only spaces in one former (non)kingdom effected. VOID
    // we expicitly update the kingdoms instead of automatically b/c
    // 1. for efficiency, sometimes many mutations in a row.
    // 2. internal conflicts, easier to find out who attacker is.
    void updateKingdoms();
    void updateKingdoms( Location s );
    // removed b/c only used 3% of the time and the work would be
    // just as hard as default implementation.
//    void updateKingdoms( Kingdom k );

    // methods to find a Space or Kingdom based on some property.
    // space from space returns the up-to-date space
    // corresponding to the row and column of s.
    // get{N/S/E/W}Space used exclusively for monument building.
    Space getSpace( int row, int col ); // redundant-but useful.
    Space getSpace( Location s );
    Space getSpace( Leader l );
    Space getUpperLeftSpace( Monument m );
// ***         
    Kingdom getKingdom( Location s );
    int getKingdomNum( Location s );
// removed b/c just a call to getSpace( Leader ) and getKingdom( Space )
//    Kingdom getKingdom( Leader l );
    Space [] getAdjacentSpaces( Location s );
    Space [] getAdjacentSpaces( Kingdom king ); // used once by ai.
    Space getEastSpace( Location s );
    Space getSouthSpace( Location s );
    Space getWestSpace( Location s );    
    Space getNorthSpace( Location s );

    Space [] getLeaderSpaces( int dynasty, Sphere [] spheres );

    // get counts of things on the board.
    // usually used for game state checks.
    int getNumCatastrophes();
    int getNumTreasures();
    
    // used to determine the kingdoms
    // in conflict and the legality
    // of a tile or leader placement.
    // Kingdoms will become stale exactly
    // when updateKingdoms is called.
    // getNum would only have been used twice
    // so it's not so bad to always create kingdom objects.
// ***         
    Kingdom [] getAdjacentKingdoms( Location s );
    int getNumAdjKingdoms( Location s );

    // needed to assign VP's at the end of each turn.
    // both are needed, because we can get monuments from
    // kingdoms, but having both can be faster.
    Monument [] getMonuments();
// ***         
    Kingdom [] getKingdoms();

    // these methods are used to check for 2x2 squares
    // of like tiles that could be built into monuments.
    // they are only used once in makeMove and isLegal.
    // almost monument pattern matching is not considered now.
    boolean isInTileSquare( Location loc );      // check 4 locations
    boolean isTileSquare( Location upper_left ); // check just UL.

    // used for red external conflict special case.
    boolean hasAdjacentLeader( Location sp );

    boolean isKingdomEqual( Location sp1, Location sp2 );

    // used in checking for removal of orphans,
    // internal conflicts, and legality of placing leaders.
    // note: internal support can include spaces outside a kingdom.
    // more general the getInternal support at about the same cost.
    int getNumAdjacentTiles( Location space, Sphere sphere );

   // determine if the space is a break point
   boolean isLocalBreak( Location sp );

    // methods that are not strictly needed for the rules
    // but are properties of board and usefull for AI.
    // getDistances for sets of spaces could be
    // creatively implemented with A* or simmilar.
    int getDistance( Location s1, Location s2 );
    int getDistance( Location s1, Kingdom k2 );
    int getDistance( Kingdom k1, Kingdom k2 );
    Space [] getTreasureSpaces();
// ***         
    Kingdom [] getNonKingdoms(); // "kingdoms" without leaders.

    //implements TileHolder interface...
    //boolean hasTiles();
    //int getNumTiles();
    //int getNumTiles( Sphere sp );    

    /*
    // NO - didn't make the cut.
    // only used in generate monument decision and very copy-wasteful.
    Space [] getSpaces();
    Space [] getInternalSupportSpaces( Space space ); // never used.
    Space [] getAdjacentSpaces( Space space, Sphere sphere ); // possible alternative
    int getNumAdjacentKindoms( Space s );
    Space getSpace( String id ); // REMOVED ATTRIBUTE
    Space getRowCol( Space s ); // not necessary with space row/col
    int getDistance( int x1, int y1, int x2, int y2 ); // private?
    int getDistance( int x1, int y1, Space s2 );  // redundant.
    Space [] getAdjacentSpaces( int row, int col ); // redundant
    boolean isInPotentialMonument( String id ); // REMOVED ATTRIBUTE
    boolean isInPotentialMonument( int row, int col ); // redundant.
    boolean isInPotentialMonument( Space s ); // named changed.
    Kingdom [] getAdjacentKingdoms( int row, int col ); // redundant.
    // better encapsulation by getting Spaces from Kingdom
    // and checking each one. this could be a private method in Game
    int removeNonSupportingTiles( Kingdom k, Sphere s );
    Space [] getAdjacentSpaces( Space [] sps ); // never used.
    */
}
