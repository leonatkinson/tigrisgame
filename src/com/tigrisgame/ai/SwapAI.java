/* File: SwapAI.java
 * Description:  AI that justs swaps out tiles.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.*;

public class SwapAI extends DefaultGameObserver implements Runnable
{
    public static boolean debug = false;
    int swapPerTurn = 12;
    GameServer gameServer;

    private SwapAI()
    {
	// default constructor
	// need to set the game to use.
    }
    
    public SwapAI( GameServer g )
    {
	// plays the dynasty specified in the specific game
	this.gameServer = g;
    }

    public void setSwapPerTurn( int swap )
    {
	swapPerTurn = swap;
    }

    public void yourMove()
    {
	// start a new thread so we don't have feedback loops of calls
	Thread t = new Thread( this );
	t.start();
    }

    public void run()
    {
	try {
	    Move move = chooseAction();
	    gameServer.makeMove( this, move );
	} catch (Exception ex) {
	    ex.printStackTrace();
	}
    }

    public void gameEnd()
    {
	if ( debug )
	    System.out.println("SwapAI: gameStart");
	
    }

    private Move chooseAction()
    {
	TigrisGame game = getGame();
	int dynasty = game.getActionDynasty();
	TileBag tiles =  new TileBag( game.getPlayer( dynasty ));
	int numToSwap; 

	if ( game.getActionNumber() == 1 ) {
	    numToSwap = swapPerTurn;
	}
	else {
	    numToSwap = swapPerTurn - 6;
	}

	while ( tiles.getNumTiles() > numToSwap &&
		tiles.hasTiles() ) {

	    // reduce the number from what we have in our screen
	    // to what we want to swap.
	    tiles.drawTile( (long) 0 );
	}

	// the Swap guy just swaps his tiles after thinking.
	if ( debug )
	    System.out.println("SwapAI: i'm swapping " + 
			       tiles.getNumTiles() );
	
	return Move.newSwapTiles( dynasty, tiles );
    }

    public static void main( String args[] ) throws Exception 
    {

	GameServerImpl server = new GameServerImpl();
	SwapAI swap0 = new SwapAI( server );
	SwapAI swap1 = new SwapAI( server );

	swap0.setSwapPerTurn( 2 );
	swap1.setSwapPerTurn( 7 );

	server.newGame( 2 );

	server.assignPlayer( swap0, 0 );
	server.assignPlayer( swap1, 1 );

	server.startGame();
    }
}



