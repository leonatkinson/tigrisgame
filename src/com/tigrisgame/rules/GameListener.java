/* File: GameListener.java
 * Description:  Listener interface for Game Events
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

public interface GameListener
{
    /************************
     * Primary Player Moves *
     ************************/

    public void leaderPlaced( Leader leader, Location space );
    public void leaderMoved( Leader leader, Location space, Location space2 );
    public void leaderWithdrawn( Leader leader );
    public void tilePlaced( int dynasty, Sphere sphere, Location space );
    public void tilesSwapped( int dynasty, TileHolder tiles );
    public void catastrophePlaced( int dynasty, Location space, Sphere tile );

    /**************************
     * Secondary Player Moves *
     **************************/

    /* assume that "forced" moves where only one choice is available
     * are always needed by the game object, and they will be made 
     * and undo'd just like normal moves.
     */

    public void monumentBuilt( int dynasty, Monument mon, Location space );
    public void noMonumentBuilt( int dynasty );
    public void treasureTaken( int dynasty, Location space );
    public void attackSupportSet( int dynasty, int board, int screen );
    public void defendSupportSet( int dynasty, int board, int screen );
    public void conflictChosen( int dynasty, Sphere sphere );

    /*********************
     * Move Consequences *
     *********************/

    public void leaderOrphaned( Leader leader, Location space );
    public void tilesDrawn( int dynasty, TileHolder tiles ); // maybe hidden.
    public void tileVPAwarded( int dynasty, Sphere sphere, Sphere sphere2 );

    /*************************
     * Conflict/Join Related *
     *************************/

    public void kingdomJoinCaused( Location space, Kingdom king1, 
				   Kingdom king2 );
    public void kingdomJoinResolved( Location space );

    public void externalConflict( Leader attacker, Leader defender );
    public void internalConflict( Leader attacker, Leader defender );
//     public void tileRemoved( Location space );
    public void conflictVPAwarded( int dynasty, Sphere sphere, int num );
    public void conflictWon( int dynasty, Sphere sphere );
    public void conflictLost( int dynasty, Sphere sphere );

    /**********************
     * End of Action/Turn *
     **********************/

    public void monumentVPAwarded( int dynasty, Sphere sphere ); // at turn end
    public void actionFinished( int dynasty );
    public void turnFinished( int dynasty );
    public void gameOver( int treasures );
}
