/* File: PlayerScoreSorter.java
 * Description:  Sorts a list of Players based on E&T scores.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.util;

import com.tigrisgame.rules.Player;

// as sorter that will rank winners greater than losers.
// if scores are exactly equal (a rare case) it will 
// rank them the same.
public class PlayerScoreSorter implements ArrayUtils.Sorter
{
   public boolean allowTies = false;
   public int penalty = 0; // number of seconds between VP penalties.

   // answers the question, "did player1 lose to player2?"
   public boolean lteq( Object obj1, Object obj2 )
   {
      Player player1 = (Player) obj1;
      Player player2 = (Player) obj2;
      
      int [] final1 = player1.getFinalVictoryPoints();
      int [] final2 = player2.getFinalVictoryPoints();
      
      // subtract based on expired time.  each penalty takes
      // off one from the lowest score.  (possibly making it negative).
      // 0 time should be first point of penalty
      if ( penalty != 0 ) {
	 final1[0] += Math.min( 0, (player1.getClock() - penalty) / penalty );
	 final2[0] += Math.min( 0, (player2.getClock() - penalty) / penalty );
      }

      for ( int ii=0; ii<final1.length; ii++ ) {
	 if ( final1[ii] < final2[ii] ) 
	    return true;
	 else if ( final1[ii] > final2[ii] )
	    return false;
	 else
	    continue;
      }
      
      if ( allowTies )
	 return true;
      else
	 // use dynasty order as a tiebreaker. (last to play wins)
	 return ( player1.getDynasty() <= player2.getDynasty() );
   }
   
   // a trick i learned at UPenn.  hehe.
   public boolean eq( Object obj1, Object obj2 )
   {
      return lteq( obj1, obj2 ) && lteq( obj2, obj1 );
   }
}
