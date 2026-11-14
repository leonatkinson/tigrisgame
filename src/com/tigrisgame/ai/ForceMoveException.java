/* File: ForceMoveException.java
 * Description:  Exception to force a move when time to think has expired.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.ai;

public class ForceMoveException extends Exception
{
    ForceMoveException( String error )
    {
	super( error );
    }
}
