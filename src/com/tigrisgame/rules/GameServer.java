/* File: GameServer.java
 * Description:   Interface for playing a game once it has been created
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTE:
 * The reason you'd use this instead of just a Game is if you 
 * need another abstraction layer to handle hidden information.
 */

package com.tigrisgame.rules;


public interface GameServer
{
    // the method to progress the game along by making moves.
    // throws exception if game not in progress, wrong player, or illegal move.
    public void makeMove( GameObserver obs, Move move )
	throws IllegalMoveException, ServerException;

    // undoMove support will attempt to undo the last move made.
    // first, a request will be send to all the players in the game
    // if they accept or reject this.
    // if all observers accept, the game will be restarted by 
    // calling gameStart( g ), showScreen( ... ), and yourMove().
    // if any observers reject, an exception will be thrown.
    public void undoMoves( GameObserver obs, int number )
	throws ServerException;

    // get a history of all moves made, including comments.
    // if the game is not over yet, the tiles will be removed
    // from the history before it is returned.
    public MoveHistory getMoveHistory();

    // get history from a players perspective.
    // even if the game is not over, 
    // the tiles from obs's perspective will be preserved.
    public MoveHistory getMoveHistory( GameObserver obs );
}  
