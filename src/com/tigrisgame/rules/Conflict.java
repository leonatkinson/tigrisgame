/* File: Conflict.java
 * Description:  An internal class of Game broken out to reduce complexity.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

// Conflicts are constructed at three times.
// 1) in response to a ChooseSphere move
// 2) in response to a PlaceTile if kingdoms are joined.
// 3) in response to a PositionLeader move if leader opposing leader present.

package com.tigrisgame.rules;

import java.io.Serializable;

public class Conflict implements Serializable, Cloneable
{
    private boolean internal = false;

    // the sphere of both objects should be the same.
    private Leader attacker;
    private Leader defender;
    
    // tiles that support the leaders on the board.
    // number remains -1 until support is chosen.
    private int [] support = new int [4];
    public static final int ATTACK_BOARD = 0;
    public static final int DEFEND_BOARD = 1;
    public static final int ATTACK_SCREEN = 2;
    public static final int DEFEND_SCREEN = 3;
    
    //private int victoryPoints;
    //Game game;

    public Conflict( Leader attacker, Leader defender ) 
    {
	this.attacker = attacker;
	this.defender = defender;
    }

    private Conflict( TigrisGame game, Leader attacker,
		      Leader defender, boolean internal )
    {
	//this.game = game;

	this.attacker = attacker;
	this.defender = defender;

	if ( attacker == null || 
	     defender == null ) {
	    throw new RuntimeException( "conflict passed null leader(s) " +
					attacker + ", " + defender);
	}

	this.internal = internal;

	// calculate the board support.
	Board board = game.getBoard();
	Space attackSpace = board.getSpace( attacker );
	Space defendSpace = board.getSpace( defender );

	if ( internal ) {
	    support[ ATTACK_BOARD ] = board.getNumAdjacentTiles( attackSpace,
								 Sphere.RED );
	    support[ DEFEND_BOARD ] = board.getNumAdjacentTiles( defendSpace,
								 Sphere.RED );
	} 
	else {
	    Kingdom kingAttack = board.getKingdom( attackSpace );
	    Kingdom kingDefend = board.getKingdom( defendSpace );
		
	    support[ ATTACK_BOARD ] = kingAttack.
		getNumTiles( attacker.getSphere() );
	    support[ DEFEND_BOARD ] = kingDefend.
		getNumTiles( defender.getSphere() );
	}	

	support[ ATTACK_SCREEN ] = -1;
	support[ DEFEND_SCREEN ] = -1;
    }

    // If getAdjKing is too slow, maybe pass in game, king, king, sphere?
    public static Conflict newExternalConflict( TigrisGame game, Sphere conflictSphere )
    {
	Board board = game.getBoard();
	Kingdom [] adjKings = board.getAdjacentKingdoms( game.getLastSpace() );

	// first, let's introduce the combatants.
	Player pA = game.getPlayer( adjKings[0].
				    getLeader( conflictSphere ).
				    getDynasty());
	Player pB = game.getPlayer( adjKings[1].
				    getLeader( conflictSphere ).
				    getDynasty());

	// what kingdom does the lucky attacker live in?
	// milt would be proud.
	// basically, if the external attacker is player pA then
	// the attacking kingdom is adjKings[0] and we use 0
	// as attackNum, the index of the attacking kingdom when
	// we construct the Conflict obect.
	int attackNum;
	if ( pA.getDynasty() == 
	     getExternalAttacker( pA.getDynasty(), 
				  pB.getDynasty(), 
				  game.getActionDynasty() ))
	    attackNum = 0;
	else
	    attackNum = 1;
	
	return new Conflict( game,
			     adjKings[ attackNum ].getLeader( conflictSphere ),
			     adjKings[ (attackNum+1)%2 ].
			     getLeader( conflictSphere ),
			     false );
    }

    public static Conflict newInternalConflict( TigrisGame game, 
					 Leader attacker, 
					 Leader defender )
    {
	return new Conflict( game, attacker, defender, true );
    }

    // the external attacker is the first to move amoung two players.
    // find the first person to move from the 2 choices
    // move to the right (or in this case i=(i+1)%players)
    // until the player is one of the participating parties.
    public static int getExternalAttacker( int dyn1, int dyn2, 
					   int actionDynasty )
    {
	// dynasty numbers can't be the same, so we check based on postition
	if ( dyn1 < dyn2 ) {
	    // if it isn't one's turn yet and two moves after one, 
	    // OR if two already went, but we haven't wrapped around yet
	    // THEN p1 is the attacker.
	    if ( actionDynasty <= dyn1 ||
		 actionDynasty > dyn2 )
		return dyn1;
	    // otherwise one has already gone,
	    // but not yet two, so p2 is the attacker.
	    else 
		return dyn2;
	} else if ( dyn1 > dyn2 ) {
	    // if it isn't two's turn yet and one moves after two, 
	    // OR if one already went, but we haven't wrapped around yet
	    // THEN p2 is the attacker.
	    if ( actionDynasty <= dyn2 ||
		 actionDynasty > dyn1 )
		return dyn2;
	    // otherwise one has already gone, but not yet two,
	    // so p2 is the attacker.
	    else 
		return dyn1;
	} else {
	    error( "can't have conflict with the same dynasty");
	    return -1;
	}
    }

    // system provided shallow copy.
    // plus copy of our array
    public Object clone()
    {
	try {
	    Conflict dest = (Conflict) super.clone();

	    dest.support = new int[4];
	    System.arraycopy( support, 0, dest.support, 0, 4 );

	    return dest;
	}
	catch (CloneNotSupportedException e) {
	    // this shouldn't happen, since we are Cloneable
	    return null;
	}
    }

    // returns the number of tiles necessary for the
    // defender to tie the attacker.
    // could be negative if less are needed to tie.
    public int getNumTilesToTie()
    {
	int attackSupport = support[ ATTACK_BOARD ];
	int defendSupport = support[ DEFEND_BOARD ];

	attackSupport += Math.max( support[ ATTACK_SCREEN ], 0 );
	defendSupport += Math.max( support[ DEFEND_SCREEN ], 0 );

	return attackSupport - defendSupport;
    }

    // sphere of the leaders fighting
    public Sphere getLeaderSphere()
    {
	return new Sphere( attacker.getSphere() );
    }

    // sphere of the tiles of support 
    public Sphere getSupportSphere()
    {
	if ( internal )
	    return Sphere.RED;
	else
	    return getLeaderSphere();
    }

    public boolean isExternal()
    {
	return !internal;
    }

    public boolean isInternal()
    {
	return internal;
    }

    public Leader getAttacker()
    {
	return attacker;
    }
    
    public Leader getDefender()
    {
	return defender;
    }

//     public int getVictoryPoints()
//     {
// 	return victoryPoints;
//     }

    public int[] getSupportArray()
    {
	return (int[]) support.clone();
    }

    // returns -1 if the attacker/defender has not yet chosen supporters,
    // otherwise returns the amount of support.
    public int getSupport( boolean attacker, boolean board )
    {
	if ( attacker ) {
	    if ( board )
		return support[ ATTACK_BOARD ];
	    else
		return support[ ATTACK_SCREEN ];
	}
	else {
	    if ( board )
		return support[ DEFEND_BOARD ];
	    else
		return support[ DEFEND_SCREEN ];
	}
    }

    // convinience methods...
    public int getAttackBoardSupport()
    {
	return getSupport( true, true );
    }

    public int getDefendBoardSupport()
    {
	return getSupport( false, true );
    }

    public int getAttackScreenSupport()
    {
	return getSupport( true, false );
    }

    public int getDefendScreenSupport()
    {
	return getSupport( false, false );
    }

    void setScreenSupport( boolean attacker, TileHolder th )
    {
	if ( attacker )
	    support[ ATTACK_SCREEN ] = (th == null ? 0 : th.getNumTiles());
	else
	    support[ DEFEND_SCREEN ] = (th == null ? 0 : th.getNumTiles());
    }

    // convienience methods.
    public void setAttackScreenSupport( TileHolder th )
    {
  	setScreenSupport( true, th );
    }

    public void setDefendScreenSupport( TileHolder th )
    {
 	setScreenSupport( false, th );
    }

    // ========== TEMP for testing ==========
    public void setSupportXXX( int idx, int num )
    {
	support[idx] = num;
    }

    // slightly redundant code from resolve, but that's okay.
    public Leader getLoser()
    {
	if ( support[ ATTACK_BOARD ] + support[ ATTACK_SCREEN ] >
	     support[ DEFEND_BOARD ] + support[ DEFEND_SCREEN ] )
	    return defender;
	else
	    return attacker;
    }

    public Leader getWinner()
    {
	if ( support[ ATTACK_BOARD ] + support[ ATTACK_SCREEN ] >
	     support[ DEFEND_BOARD ] + support[ DEFEND_SCREEN ] )
	    return attacker;
	else
	    return defender;
    }

    public static void error( String msg )
    {
	throw new RuntimeException();
    }
    
    public String toString()
    {
	String str;
	
	str = "Conflict ";
	if ( internal )
	    str += "Internal ";
	else
	    str += "External ";
	str += "Attacker is " + attacker + " with " +  support[ ATTACK_BOARD ] 
	    + " and " + support[ ATTACK_SCREEN ];
	str += ", Defender is " + defender + " with " + support[ DEFEND_BOARD ]
	    + " and " + support[ DEFEND_SCREEN ];
	return str;
    }
}



