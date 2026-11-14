/* File: AIPlayer.java
 * Description:  An interface for the ai display.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

import com.tigrisgame.rules.Move;
import com.tigrisgame.rules.TigrisGame;

public interface AIPlayer
{
    // basic evaluate and generate that is the basis of the AI.
    // from these functions, different
    double [] evaluateGame( TigrisGame game );
    Move [] generateMoves( TigrisGame game );

    // other usefull possibly supported metrics and fun
    int getPercentDone();
    boolean isThinking();
    String getName();

    /* MAYBE ???
       int getSearchTreeSize();  
       --- multithreading nightmare! the tree will grow as we check the size
       Move getBestMove();
       int getGamesEvaluated();
       double getBestEval();

    */
}
