/* File: MovePlan.java
 * Description:   parses rules objects from strings.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.Move;

import java.util.ArrayList;

public class MovePlan
{
   double value = Double.NEGATIVE_INFINITY;
   ArrayList moves = new ArrayList();

   public MovePlan( Move move, double val )
   {
      this.moves.add( move );
      this.value = val;
   }

   public Move getMove( int idx )
   {
      if ( moves.size() > 0 )
	 return (Move)moves.get(idx);
      else
	 return null;
   }
}

