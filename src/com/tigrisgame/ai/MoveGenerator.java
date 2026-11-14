/* File: MoveGenerator.java
 * Description:  an interface for the AI programs that generates moves from a position.
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

public interface MoveGenerator
{
    public Move [] getMoves( TigrisGame game );
}
