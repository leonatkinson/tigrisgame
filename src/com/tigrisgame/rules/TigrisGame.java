/* File: TigrisGame.java
 * Description:  Game state for E&T Game (interface)
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* 
 * A complete instantaneous description of the game is follows:
 * 2 to 4 Players, each with
 * - name
 * - vp for all spheres, treasures, and current tiles.
 * (tiles are hidden type if partial view)
 * a Board 11 (row) x16 (col) Spaces
 * - each Space is either empty land / empty water or contains
 *   a leader / tile / catastrophe / monument with appropriate sphere(s).
 * - one space is marked as "last action"
 * (kingdom information is not stored)
 * Game state variables as follows:
 * - action number
 * - action dynasty
 * - move dynasty
 * - game state (what type of move expected)
 * - tile bag exhausted flag.
 * Tile Bag, described as an ordered sequence of tiles for repeatability.
 * (tiles are type hidden if partial view).
 * Conflict with the following fields, or null conflict.
 * - leaders sphere
 * - attack/defend dynasty
 * - attack/defend board support
 * - attack/defend played support.
 */

package com.tigrisgame.rules;

public interface TigrisGame
{
    // the game state.
    public static final int
	WAIT_ACTION         = 1,
	WAIT_CONFLICT_ORDER = 2,
	WAIT_ATTACK_SUPPORT = 3,
	WAIT_DEFEND_SUPPORT = 4,
	WAIT_MONUMENT       = 5,
	WAIT_TREASURE       = 6,
	GAME_OVER           = 7;

    // copy construstor helper method.
    void init( TigrisGame game );
    void init( GameOptions options );
    Object clone();

    // the main interface to mutate the game.
    void makeMove( Move move ) throws IllegalMoveException;
    //void makeLegalMove( Move move ); // same w/o checks.
    void isLegal( Move move ) throws IllegalMoveException;
    Rules getRules();

   // access function for basic game state.
   int getState();
   void setState(int state);

   int getActionNumber();
   void setActionNumber( int num );
   
  // who is currently making actions.
   int getActionDynasty();
   void setActionDynasty( int dyn );

    // who will make the next action or subaction.
   int getMoveDynasty();
   void setMoveDynasty( int dyn );

   Space getLastSpace(); // used for a bunch of purposes, set in makeMove.

   // standar access functions.
   Board getBoard();
   Conflict getConflict();
   void setConflict( Conflict conflict );

   //Player [] getPlayers();
   int getNumPlayers();
   Player getPlayer( int dynasty );
   Player getPlayer( Leader l );
   
   // instead of returning a mutable tile bag, we expose the hideTiles method
   // readonly getTileBag and getTotalTiles are also provided.
   void hideTiles( int view );
   TileHolder getTileBag();
   TileHolder getTotalTiles();
   boolean fillPlayerTiles();
   boolean getTileBagWasExhausted();
   void setTileBagWasExhausted( boolean val );

//     TileBag getDiscardPile();

   void fireGameEvent( int type, Object obj1, Object obj2, Object obj3 );
   void fireGameEvent( GameEvent event );
   void addGameListener( GameListener listener );
   void removeGameListener( GameListener listener );
}
