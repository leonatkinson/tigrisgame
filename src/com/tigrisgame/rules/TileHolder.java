/* File: TileHolder.java
 * Description:  Interface for holder of tiles (player, bag, etc)
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * Since the Tile class has HIDDEN as a tile type that is implicitly 
 * handled in this class as if it were another type of tile with color Hidden.
 * it can be thought of as a "face down tile".  however, no additional information
 * such as probabilities is implied by this type.
 */

package com.tigrisgame.rules;

public interface TileHolder
{
    boolean hasTiles(); // aka !isEmpty() or getNumTiles() != 0
    int getNumTiles(); // Sum of all spheres and unknowns.
    int getNumTiles( Sphere sp );
//     int getNumTiles( Tile tl );
}
