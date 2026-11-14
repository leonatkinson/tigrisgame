/* File: GameSetupServer.java
 * Description:  Interface for setting up a game.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * interface for starting, joining and observing a game.
 * once the game has been created a GameServer instance will
 * handle actually playing the game.
 * to properly use an instance, 
 * first: newGame() OR loadGame() to choose a game object.
 * second: joinGame() to assign players.
 * third: startGame()
 * then: makeMove() until the game is over
 * observeGame(), unobserveGame() can be called at any time.
 * saveGame() can be called anytime after a game is loaded or created.
 */

package com.tigrisgame.rules;


public interface GameSetupServer extends GameServer
{
    // methods for creating and joining a game.
    // if these methods are called before the game is started the game object
    // will be set and the players cleared.
    // if they are called during the game they
    // will be ignored (exception thrown). 
    // after the game is over they may be called to start another game.
   public void newGame() throws ServerException;
   public void newGame( int numPlayers ) throws ServerException;
   public void newGame( GameOptions options ) throws ServerException;
   public void loadGame( String filename ) throws ServerException;
   
    // assign players to a particular dynasty
    // throws exception if game started, invalid params, 
    // or player already assigned.
    public void assignPlayer( GameObserver obs, int dynasty ) 
	throws ServerException;

    // start the game and send the game state 
    // to all the players and observers
    // if the number of players joined is different than the
    // number needed the game will not start.
    // throws exception if game in progress or not ready.
    public void startGame() throws ServerException;
    
    // other fun and usefull methods.
    // these methods are silent on error.
    public void saveGame( String filename );
    public void observeGame( GameObserver obs );
    public void unobserveGame( GameObserver obs );
}  
