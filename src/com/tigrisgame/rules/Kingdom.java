/* File: Kingdom.java
 * Description:  Game state for Kingdom (interface) - group of tiles on a Board.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES: 
 * strictly a kingdom object isn't necessary if the board knows about
 * and maintains the "kingdoms".  You could for instance give any
 * space on the board as and lookup what kingdom it is in instead of
 * floating around kingdom references.
 * Also used for "non-kingdoms", leaderless groups of tiles
 */

package com.tigrisgame.rules;

public interface Kingdom extends TileHolder
{
    // methods to access Spaces or other
    // contents of the kingdom.
    boolean isKingdomEqual( Kingdom k );
    int getKingdomNum();
    Leader getLeader( Sphere s ); 
    int getNumLeaders();

    // shortcut for external conflicts.
    // returning Leader [] might help in some implementations,
    // but it isn't used.
    Sphere [] getConflictingSpheres( Kingdom k );

    // used once now, but in general can be usefull.
    Monument [] getMonuments();

    // used to resolve external conflicts
    // when spaces need to be removed.
    Space [] getTileSpaces( Sphere s );
    // gets all spaces including leaders, monuments, and tiles.
    Space [] getSpaces();

    // necessary for treasure taking rules.
    // also, well placed in this interface.
    int getNumTreasures();
    Space [] getTreasureSpaces();

    // tests if a space is in a kingdom.
    boolean hasSpace( Location s );
}
