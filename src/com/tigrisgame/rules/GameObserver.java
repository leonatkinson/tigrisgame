/* File: GameObserver.java
 * Description:  Interface to get updates from a GameServer.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * Interface to be implemented by GUI and AI components
 * that interact with a game server to get information 
 * through callback updates about a game.
 * Only handles the game of one game at a time.
 * The game "session" is defined by first calling gameStart(), then
 * some number of moveMade(), showScreen(), and yourMove(), followed by
 * a gameEnd() which allows the possibility for another gameStart().
 * a seperate class could have multiple listeners to handle multiple games.
 */

package com.tigrisgame.rules;

public interface GameObserver
{
   // essential methods for starting and progressing a game.
   // this game passed to gameStart is assumed to be mutable-safe.
   void gameStart( TigrisGame g );
   void moveMade( Move move );
   void showScreen( int dynasty, TileHolder tiles );
   void showClock( int dynasty, long clock );

    // other usefull methods
    // perhaps yourMove could give a time left to make move ??
    // gameEnd is passed the dynasties sorted by finish order (winner at 0)
    void yourMove();
    boolean requestUndoMoves( int number );
    void gameEnd( int [] dyns );
    String getName();
}
