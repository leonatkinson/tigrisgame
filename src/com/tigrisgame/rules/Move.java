/* File: Move.java
 * Description:  Container for all state needed to effect a Game change.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * Stores the details of a player decision such that
 * it can be used to uniquely identify the
 * decision (move) in the context of a game.
 * A chess equivalent would be "black pawn to king four".
 * Note player IS included, but
 * the move number is NOT (well, i guess it's presence is debatable) 
 *
 * Valid Properties change depending on the type of move.
 * The following matrix shows which properties are valid.
 * If an invalid property is queried, and warning message will be printed.
 * In the future, it may throw an exception.
 *
 *                    dynasty space sphere sphere2 tiles
 * INVALID                                      
 * POSITION_LEADER       X      X      X        
 * PLACE_TILE            X      X      X        
 * PLAY_CATASTROPHE      X      X               
 * SWAP_TILES            X                           X 
 * CHOOSE_SPHERE         X             X        
 * CHOOSE_SUPPORTERS     X                           X 
 * BUILD_MONUMENT        X      X      X      X  
 * CHOOSE_TREASURE       X      X               
 */

package com.tigrisgame.rules;

import java.io.Serializable;

public class Move implements Serializable
{
    public static final int 
	INVALID          = -1,
	// main actions
	POSITION_LEADER  = 1,
	PLACE_TILE       = 2,
	PLAY_CATASTROPHE = 3,
	SWAP_TILES       = 4,
	// secondary actions
	CHOOSE_SPHERE    = 5,
	CHOOSE_SUPPORTERS= 6,
	BUILD_MONUMENT   = 7,
	CHOOSE_TREASURE  = 8;
     
    private int dynasty;
    private int type = INVALID;
    private TileBag tiles = null;
    // space just used as a reference for row/col
    private Location space = null;
    private Sphere sphere = null;
    private Sphere sphere2 = null;
    
    public Move()
    {
	type = INVALID;
    }

    private Move( int dynasty, int type, Location space,
		  Sphere sphere, Sphere sphere2, TileHolder tiles )
    {
	this.type = type;
	this.space = space;
	this.dynasty = dynasty;
	// make our own copies of the objects to ensure that they 
	// are truely private variable that only this class can modify
	// and also ensure that they are serializable
	this.space  = (space  == null) ? null : new LocationImpl( space.getRow(), space.getCol());
	this.sphere = (sphere == null) ? null : new Sphere( sphere );
	this.sphere2 = (sphere2 == null) ? null : new Sphere( sphere2 );
	this.tiles  = (tiles  == null) ? null : new TileBag( tiles );
    }

    public Object clone()
    {
  	try
  	    {
  		Move copy = (Move) super.clone();
		copy.tiles = new TileBag( tiles );
		return copy;
  	    }
  	catch (CloneNotSupportedException e)
  	    {
  		// this shouldn't happen, since we are Cloneable
  		return null;
  	    }
    }

    public static Move newPositionLeader( int dynasty, Location space, 
					  Sphere sphere )
    {
	Move move = new Move( dynasty, POSITION_LEADER, 
			      space, sphere, null, null );
	return move;
    }

    public static Move newPlaceTile( int dynasty, Location space, 
				     Sphere sphere )
    {
	Move move = new Move( dynasty, PLACE_TILE, space, 
			      sphere, null, null );
	return move;
    }

    public static Move newPlayCatastrophe( int dynasty, Location space )
    {
	Move move = new Move( dynasty, PLAY_CATASTROPHE, 
			      space, null, null, null );
	return move;
    }
    
    public static Move newSwapTiles( int dynasty, TileHolder tiles )
    {
	Move move = new Move( dynasty, SWAP_TILES,
			      null, null, null, tiles );
	return move;
    }
    
    // order of conflict
    public static Move newChooseSphere( int dynasty, Sphere sp )
    {
	Move move = new Move( dynasty, CHOOSE_SPHERE, 
			      null, sp, null, null );
	return move;
    }

    public static Move newBuildMonument( int dynasty, Location space, 
					 Sphere primary, Sphere secondary )
    {
	Move move = new Move( dynasty, BUILD_MONUMENT, space, 
			      primary, secondary, null);
	return move;
    }

    public static Move newChooseSupporters( int dynasty, TileHolder supporters)
    {
	Move move = new Move( dynasty, CHOOSE_SUPPORTERS, 
			      null, null, null, supporters );
	return move;
    }


    public static Move newChooseTreasure( int dynasty, Location space )
    {
	Move move = new Move( dynasty, CHOOSE_TREASURE, 
			      space, null, null, null );
	return move;
    }

   public boolean isAction() { return ( type == POSITION_LEADER || 
					type == PLACE_TILE || 
					type == SWAP_TILES || 
					type == PLAY_CATASTROPHE ); }
   public boolean isPositionLeader()  { return ( type == POSITION_LEADER ); }
   public boolean isPlaceTile()       { return ( type == PLACE_TILE); }
   public boolean isPlayCatastrophe() { return ( type == PLAY_CATASTROPHE ); }
   public boolean isSwapTiles()       { return ( type == SWAP_TILES ); }
   public boolean isChooseSphere()    { return ( type == CHOOSE_SPHERE); }
   public boolean isBuildMonument()   { return ( type == BUILD_MONUMENT ); }
   public boolean isChooseSupporters(){ return ( type == CHOOSE_SUPPORTERS ); }
   public boolean isChooseTreasure()  { return ( type == CHOOSE_TREASURE ); }

   public int getDynasty()
   {
      if ( type == INVALID ) {
	 System.out.println("Warning: Move.getDynasty() invalid for " 
			    + typeToString( type ) );
	 throw new RuntimeException();
      }
      
	return dynasty;
   }

    public int getType()
    {
	return type;
    }

    public Location getLocation()
    {
  	if ( type != POSITION_LEADER &&
  	     type != PLACE_TILE &&
  	     type != PLAY_CATASTROPHE &&
  	     type != BUILD_MONUMENT &&
  	     type != CHOOSE_TREASURE ) {
   	    System.out.println("Warning: Move.getLocation() invalid for " 
 			       + typeToString( type ) );
	}
   	return space;
    }

    public Sphere getSphere()
    {
	if ( type != POSITION_LEADER &&
	     type != PLACE_TILE &&
	     type != CHOOSE_SPHERE &&
	     type != BUILD_MONUMENT )
	    System.out.println("Warning: Move.getSphere() invalid for " 
			       + typeToString( type ) );

	return sphere;
    }

    public Sphere getSphere2()
    {
	if ( type != BUILD_MONUMENT )
	    System.out.println("Warning: Move.getSphere() invalid for " 
			       + typeToString( type ) );

	return sphere2;
    }

    public TileHolder getTiles()
    {
	if ( type != SWAP_TILES &&
	     type != CHOOSE_SUPPORTERS )
	    System.out.println("Warning: Move.getTiles() invalid for "
			       + typeToString( type ) );

	// don't give out the actual bag, create a copy.
	return new TileBag( tiles );
    }

    public static String typeToString( int i )
    {
	String typeStr;
	switch ( i ) 
	    {
		// main actions
	    case POSITION_LEADER:
		typeStr = "Position Leader";
		break;
	    case PLACE_TILE:
		typeStr = "Place Tile";
		break;
	    case PLAY_CATASTROPHE:
		typeStr = "Play Catastrophe";
		break;
	    case SWAP_TILES:
		typeStr = "Swap Tiles";
		break;
		// secondary actions
	    case CHOOSE_SPHERE:
		typeStr = "Choose Sphere";
		break;
	    case CHOOSE_SUPPORTERS:
		typeStr = "Choose Supporters";
		break;
	    case BUILD_MONUMENT:
		typeStr = "Build Monument";
		break;
	    case CHOOSE_TREASURE:
		typeStr = "Choose Treasure";
		break;
	    default:
		typeStr = "Invalid";
		break;
	    }
	return typeStr;
    }

   public String toString()
   {
      StringBuffer strbuf = new StringBuffer();
      strbuf.append( dynasty ); // should be 1 char.

      switch ( type ) {
	 // main actions
      case POSITION_LEADER:
	 strbuf.append( "L" );
	 strbuf.append( sphere.toChar() );
	 strbuf.append( LocationImpl.formatLocation( space,"" ));
	 break;
      case PLACE_TILE:
	 strbuf.append( "T" );
	 strbuf.append( sphere.toChar() );
	 strbuf.append( LocationImpl.formatLocation( space,"" ));
	 break;
      case PLAY_CATASTROPHE:
	 strbuf.append( "C" );
	 strbuf.append( LocationImpl.formatLocation( space,"" ));
	 break;
      case SWAP_TILES:
	 strbuf.append( "S" );
	 strbuf.append( tiles.getLetterString());
	 break;
	 // secondary actions
      case CHOOSE_SPHERE:
	 strbuf.append( "O" );
	 strbuf.append( sphere.toChar() );
	 break;
      case CHOOSE_SUPPORTERS:
	 strbuf.append( "A" );
	 strbuf.append( tiles.getLetterString());
	 break;
      case BUILD_MONUMENT:
	 strbuf.append( "M" );
	 if ( space == null )
	    strbuf.append("0");
	 else {
	    strbuf.append( sphere.toChar() );
	    strbuf.append( sphere2.toChar() );
	    strbuf.append( LocationImpl.formatLocation( space,"" ));
	 }
	 break;
      case CHOOSE_TREASURE:
	 strbuf.append("R");
	 strbuf.append( LocationImpl.formatLocation( space,"" ));
	 break;
      default:
	 strbuf.append( "Invalid Move" );
	 break;
      }
      return strbuf.toString();
    }

    public boolean equals( Object obj )
    {
	if ( obj != null && obj instanceof Move ) {
	    Move move = (Move) obj;

	    if ( move.type != this.type ||
		 move.dynasty != this.dynasty )
		return false;

	    // all the other fields must be equal. (space is just coords.
	    boolean tilesOkay = ( move.tiles == null ? this.tiles == null :
				  move.tiles.equals( this.tiles ));
	    boolean sphereOkay = ( move.sphere == null ? this.sphere == null :
				  move.sphere.equals( this.sphere ));
	    boolean spaceOkay = ( move.space == null ? this.space == null :
				  move.space.isLocationEqual( this.space ) );

	    return ( tilesOkay && sphereOkay && spaceOkay );
	}

	return false;
    }

    public int hashCode()
    {
	return (type * 64) & (dynasty);
    }
}



