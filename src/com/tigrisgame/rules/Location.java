/* File: Location.java
 * Description:  Wrapper for x,y location (inteface)
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

public interface Location
{
   int getRow();
   int getCol();

    // location in the board so board can easily
    // locate a space in its internal representation.
    // also testing for equality of position
   boolean isLocationEqual( Location loc );
}



