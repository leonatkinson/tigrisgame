/* File: GameEvaluator.java
 * Description:  Interface to heuristically score a game.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * an interface for scoring a game.
 * evaluate returns an int array of values corresponding
 * to the score for each dynasty.
 * The higher the number, the better the player is doing.
 * in general, does NOT give relative scores so evaluators
 * can be composed and compared by a higher logic function.
 *
 * possible implementations
 * - winning evaluator
 * scores for VP and treasures towards final score.
 * a very short sighted of the scorers but effective with a large
 * enough search tree.
 * - leader/support evaluator
 * also scores points for having leaders down on the board.
 * value of leaders is determined by how much support they have.
 * - treasure hound evaluator
 * for num treasures and closeness to next one
 * - war monger evaluator
 * scores high for drifting kingdoms closer to winning conflicts
 * - monument eval
 * scores points for controlling monuments and being on the way to 
 * building them.
 * - screen/tile eval
 * scores points for having needed (how?) tiles in your hand and catastrophes.
 *
 *
 * NOTES ON RELATIVE SCORES.
 * Integer.MAX_VALUE is the score for "won the game", while
 * last place has a score Integer.MIN_VALUE.
 * Scores for finishing a game with a mid-place in a multiplayer game
 * is not specified, and can take any value.
 *

 */

package com.tigrisgame.ai;

import com.tigrisgame.rules.TigrisGame;

public interface GameEvaluator
{
    // evalutes for all dynasties, returns an array of int scores.
    public int [] evaluate( TigrisGame game );

    // evalutates for one dynasty, returns an int.
    // may or may not consider other dynasties to get relative score.
    public int evaluate( TigrisGame game, int dynasty );
}
