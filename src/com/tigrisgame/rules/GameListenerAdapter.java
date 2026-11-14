/* File: GameListenerAdapter.java
 * Description:  Adapter implementation of GameListener, just prints out.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;


public class GameListenerAdapter implements GameListener
{
    public int verbosity = 0;
    
    /************************
     * Primary Player Moves *
     ************************/

    public void leaderPlaced( Leader leader, Location space )
    {
	message( "leaderPlaced " + leader + " at " + space );
    }   
    public void leaderMoved( Leader leader, Location space, Location space2 )
    {
	message( "leaderPlaced " + leader + " moved to " + space + " from " + space2 );
    }   
    public void leaderWithdrawn( Leader leader )
    {
	message( "leaderPlaced " + leader + " removed" );
    }   

    public void tilePlaced( int dynasty, Sphere sphere, Location space )
    {
	message( "tilePlaced by " + dynasty + " " + sphere + " at " + space );
    }

    public void tilesSwapped( int dynasty, TileHolder tiles )
    {
	message( "tileSwapped by " + dynasty + " " + tiles );
    }

    public void catastrophePlaced( int dynasty, Location space, Sphere tile )
    {
	message( "catastrophePlaced by " + dynasty + " at " + space
		 + " over tile " + tile );
    }

    /**************************
     * Secondary Player Moves *
     **************************/

    public void monumentBuilt( int dynasty, Monument mon, Location space )
    {
	message( "monumentBuild by " + dynasty + " " + mon + " at " + space );
    }

    public void noMonumentBuilt( int dynasty )
    {
	message( "noMonumentBuild by " + dynasty );
    }

    public void treasureTaken( int dynasty, Location space )
    {
	message( "treasureTaken by " + dynasty + " at " + space );
    }
    public void attackSupportSet( int dynasty,
				  int board, int screen )
    {
	message( "attackSupportSet by " + dynasty + 
		 " now has " + board + " + " + screen );
    }

    public void defendSupportSet( int dynasty, 
				  int board, int screen )
    {
	message( "defendSupportSet by " + dynasty + 
		 " now has " + board + " + " + screen );
    }

    public void conflictChosen( int dynasty, Sphere sphere )
    {
	message( "conflictChosen by " + dynasty + " " + sphere );
    }

    /*********************
     * Move Consequences *
     *********************/

    public void leaderOrphaned( Leader leader, Location space )
    {
	message( "leaderOrphaned " + leader + " from " + space );
    }
    public void tilesDrawn( int dynasty, TileHolder tiles )
    {
	message( "tilesDrawn by " + dynasty + " " + tiles );
    }
    public void tileVPAwarded( int dynasty, Sphere sphere, Sphere sphere2 )
    {
	message( "tileVPAwarded by " + dynasty + " " + sphere + " " + sphere2);
    }

    /*************************
     * Conflict/Join Related *
     *************************/

    public void kingdomJoinCaused( Location space, Kingdom king1, Kingdom king2 )
    {
	message( "kingdomJoinCaused at " + space + " between " + king1 + " and " + king2 );
    }
    public void kingdomJoinResolved( Location space )
    {
	message( "kingdomJoinResolved at " + space );
    }

    public void externalConflict( Leader attacker, Leader defender )
    {
	message( "externalConflict between " + attacker + " and " + defender );
    }
    public void internalConflict( Leader attacker, Leader defender )
    {
	message( "internalConflict between " + attacker + " and " + defender );
    }
//     public void tileRemoved( Location spaces )
//     {
// 	message( "tileRemoved " + spaces );
//     }
    public void conflictVPAwarded( int dynasty, Sphere sphere, int num )
    {
	message( "conflictVPAwarded to " + dynasty + " " + sphere + " " +num );
    }

    public void conflictWon( int dynasty, Sphere sphere )
    {
	message( "conflictWon to " + dynasty + " " + sphere  );
    }

    public void conflictLost( int dynasty, Sphere sphere )
    {
	message( "conflictLost to " + dynasty + " " + sphere );
    }

    /**********************
     * End of Action/Turn *
     **********************/

    // ??????????
//     // turn/action/game next/over/completed/finished
//     public void gameOver();
    public void monumentVPAwarded( int dynasty, Sphere sphere )
    {
	message( "monumentVPAwarded to " + dynasty + " " + sphere );
    }

    public void actionFinished( int dynasty )
    {
	message( "actionFinished for " + dynasty );
    }

    public void turnFinished( int dynasty )
    {
	message( "turnFinished for " + dynasty );
    }

    public void gameOver( int treasures )
    {
	message( "gameOver" );
    }

    public void message( String msg )
    {
	if ( verbosity > 0 ) {
	    System.out.println( "GLA: " + msg );
	}
    }
}
