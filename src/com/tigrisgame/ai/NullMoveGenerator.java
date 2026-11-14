/* File: NullMoveGenerator.java
 * Description:  Generates the types of moves made when out of time.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.Move;
import com.tigrisgame.rules.Sphere;
import com.tigrisgame.rules.TigrisGame;
import com.tigrisgame.rules.TileBag;

public class NullMoveGenerator
{
   public static Move [] getMoves( TigrisGame game )
   {
      Sphere [] spheres = Sphere.getSpheres();
      int dynasty = game.getMoveDynasty();
      Move myMove = null;

      switch ( game.getState() ) {
      case TigrisGame.WAIT_ACTION:
	 myMove = Move.newSwapTiles( dynasty, new TileBag());
	 break;
      case TigrisGame.WAIT_MONUMENT:
	 myMove = Move.newBuildMonument( dynasty, null, null, null );
	 break;
      case TigrisGame.WAIT_ATTACK_SUPPORT:
      case TigrisGame.WAIT_DEFEND_SUPPORT:
	 myMove = Move.newChooseSupporters( dynasty, new TileBag());
	 break;
      case TigrisGame.WAIT_TREASURE:
      case TigrisGame.WAIT_CONFLICT_ORDER:
	 myMove = LegalMoveGenerator.getMoves( game )[0];      
	 break;
      case TigrisGame.GAME_OVER:
	 break;
      default:
	 System.out.println("unknown game state " +
			       game.getState());
      }

      if ( myMove != null )
	 return new Move[] { myMove };
      else
	 return null;
   }
}

