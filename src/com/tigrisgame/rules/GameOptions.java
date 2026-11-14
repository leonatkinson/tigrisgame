/* File: GameOptions.java
 * Description:  encapsulates all the options for a Game constructor.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

public class GameOptions
{
    public String mapfile = null;
    public String [] playerNames = new String[2];
    public TileHolder tiles =
      new TileBag( Rules.TOTAL_BLACK, Rules.TOTAL_RED,
		   Rules.TOTAL_BLUE, Rules.TOTAL_GREEN );

   public int variant = 0;
   public int boardImpl = 2;
   public long seed = System.currentTimeMillis();
   public boolean tileDumping = true;

   // time options.
   public long baseTime = 45*60*1000;
   public int increment = 0;
   public int penalty = 0;

   public GameOptions() 
   {
      setDefaultPlayerNames();
   }
    
   public GameOptions( int numPlayers )
   {
      playerNames = new String[numPlayers];
      setDefaultPlayerNames();
   }

    void setDefaultPlayerNames()
    {
	if ( playerNames == null )
	    return;

	for ( int pp=0; pp<playerNames.length; pp ++ ) {
	    playerNames[pp] = "Player " + (pp+1);
	}
    }
}
