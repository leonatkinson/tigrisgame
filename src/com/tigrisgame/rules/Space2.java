/* File: Space2.java
 * Description:  Game state for E&T Space (implementation with binary packing)
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

import java.io.Serializable;

class Space2 extends LocationImpl implements Space, Serializable
{
    byte m_value;

   // FOUND IN SUPERCLASS!!
//     int m_row = -1;
//     int m_col = -1;

    //Definition of fields
    // byte is stored as two's compliment
    // so be careful initializing the fields
    // with 1 msb.
    // MSB -------- LSB

    // 0000 dddd Catastrophe
    // 0001 0ddd Open Land
    // 0001 1ddd Open Water
    // 001S Sddd Join Tile

    // 01TS SMdd Tile
    // 10dS SdDD Leader
    // 11TS SMSS Monument
    // T is the Treasure bit
    // M is the must take treasure bit.
    // SS are the two bits for Sphere
    
   // types are the bit fields to AND with the value
   // to get the value of a field.
   public static final byte TYPE = -64;    // compare to LEADER,MON,TILE,EMPTY
   public static final byte EMPTY = 0x00;  //includes catastrohpes.
   public static final byte TILE = 0x40;   //01dd dddd
   public static final byte LEADER = -128; //10dd dddd
   public static final byte MON = -64;     //11dd dddd

    // values for EMPTY, compare to CAT, EMPTY_LAND, EMPTY_WATER
    public static final byte EMPTY_FIELD = -8;    // 1111 1000
    public static final byte CAT         = 0x00;  // 0000 0ddd 
    public static final byte EMPTY_LAND  = 0x10;  // 0001 0ddd
    public static final byte EMPTY_WATER = 0x18;  // 0001 1ddd

    // values for JOIN_FIELD.
    // NOTE: even if pieces of a kingdom are only connected through the
    // join tile, they still count as the same kingdom.
    // therefore, we must rember which one of two kingdoms they form.
    // assume north has polarity "1", the EAST, SOUTH, and WEST bits
    // say if they are with the north or the other one.
    public static final byte JOIN_FIELD  = -32;   // 1110 0000
    public static final byte JOIN        = 0x20;  // 001d dddd
//     public static final byte EAST_JOIN   = 0x01;  // 0000 0001
//     public static final byte WEST_JOIN   = 0x02;  // 0000 0010
//     public static final byte SOUTH_JOIN  = 0x04;  // 0000 0100

    // fields for TILE/LEADER/MON
    public static final byte DYN         = 0x03; // 0000 0011
    public static final byte SPH         = 0x18; // 0001 1000
    public static final byte TREAS       = 0x20; // 0010 0000
    public static final byte MUST_TAKE   = 0x04; // 0000 0100 

    // convenience fields.
    // treasure is TREAS bit plus must also be tile or mon
    // king stuff is TILE | MON | LEADER
    public static final byte HAS_TREAS = 0x60; 
    public static final byte KING_STUFF = -64; //11dd dddd

    // Spheres values.
    // values must be 0 to 3 to fit in a 2 bit field.
    public static final byte BLACK = (byte) Sphere.BLACK_NUM;
    public static final byte RED   = (byte) Sphere.RED_NUM;
    public static final byte BLUE  = (byte) Sphere.BLUE_NUM;
    public static final byte GREEN = (byte) Sphere.GREEN_NUM;

   // default constructor
   // (actually builds a catastrophe for 0 init value)
   public Space2()
   {
      super( -1, -1 );
   }
   
   Space2( int row, int col )
   {
      super( row, col );
   }

   Space2( byte value, int row, int col )
   {
      super( row, col );
      m_value = value;
   }

   // construct byte representation
   // from old pure OO space.
   Space2( Space sp )
   {
      super( sp.getRow(), sp.getCol() );

      // start out with a blank value field.
      //m_value = 0;

	if ( sp.hasCatastrophe() )
	    {
		m_value = CAT;
	    }
	else if ( sp.isEmpty() )
	    {
		if ( sp.isLand() )
		    m_value = EMPTY_LAND;
		else
		    m_value = EMPTY_WATER;
	    }
	else if ( sp.getTile() != null )
	    {
		m_value = TILE;

		Tile t = sp.getTile();
		setSphere( getSphereInt( t ) );

		setTreasure( sp.hasTreasure() );
		setMustTake( sp.hasMustTakeTreasure() );
	    }
	else if ( sp.getLeader() != null )
	    {
		m_value = LEADER;

		Leader l = sp.getLeader();
		setSphere( getSphereInt( l.getSphere() ) );
		setDynasty( l.getDynasty() );
	    }
	else if ( sp.getMonument() != null )
	    {
		m_value = MON;

		Monument m = sp.getMonument();
		setSphere( getSphereInt( m.getFirstSphere() ));
		setDynasty( getSphereInt( m.getSecondSphere() ));

		setTreasure( sp.hasTreasure() );
		setMustTake( sp.hasMustTakeTreasure() );
	    }
	else
	    {
		throw new RuntimeException("Unknown space type in Space2( Space )");
	    }

	// dEBUG
    }
	
//     public Space1 getSpace1()
//     {
// 	Space1 space = new Space1( getRow(), getCol() );

// 	// first two bits determine type.
// 	int type = m_value & TYPE;

// 	switch ( type )
// 	    {
// 	    case EMPTY:
// 		if ( (m_value & EMPTY_FIELD) == CAT )
// 		     space.putCatastrophe();
// 		else if ( (m_value & EMPTY_FIELD) == EMPTY_LAND )
// 		    // nothing changes.
// 		    ;
// 		else if ( (m_value & EMPTY_WATER) == EMPTY_WATER )
// 		    space.river = true;
// 		break;
// 	    case TILE:
// 		space.putTile( new Tile( getIntSphere( getSphere() ) ) );
// 		space.treasure = hasTreasure();
// 		space.river = ! isLand();
// 		break;
// 	    case LEADER:
// 		space.putLeader( new Leader( getIntSphere( getSphere() ), getDynasty() ) );
// 		break;
// 	    case MON:
// 		space.putMonument( new Monument( getIntSphere( getSphere() ), getIntSphere( getDynasty())));
// 		space.treasure = hasTreasure();
// 		break;
// 	    default:
// 		System.out.println("Warning: unknown type of m_value in Space2.getSpace()");
// 	    }
// 	return space;
//     }


    public static int getSphereInt( Sphere sp )
    {
	if ( sp.isBlack() )
	    return BLACK;
	else if ( sp.isRed() )
	    return RED;
	else if ( sp.isBlue() )
	    return BLUE;
	else if ( sp.isGreen() )
	    return GREEN;

	System.out.println("Warning: Unknown sphere type in Space2.getSphereInt");
	throw new RuntimeException();
	//return -1;
    }

    public static Sphere getIntSphere( int color )
    {
	if ( color == BLACK )
	    return Sphere.BLACK;
	else if ( color == RED )
	    return Sphere.RED;
	else if ( color == BLUE )
	    return Sphere.BLUE;
	else if ( color == GREEN )
	    return Sphere.GREEN;

	System.out.println("Warning: Unknown int value in " + 
			   "Space2.getIntSphere " + color);
	throw new RuntimeException();

	//return null;
    }

    public boolean hasJoinTile()
    {
	return ( ( m_value & JOIN_FIELD ) == JOIN );
    }

//      public boolean isJoinNorthEast()
//      {
//  	return (( m_value & EAST_JOIN ) != 0 );
//      }

//     public boolean isJoinNorthSouth()
//     {
// 	return (( m_value & SOUTH_JOIN ) != 0 );
//     }

//     public boolean isJoinNorthWest()
//     {
// 	return (( m_value & WEST_JOIN ) != 0 );
//     }

//     public void setJoinTile( boolean east, boolean south, boolean west )
//     {
// 	m_value = JOIN;
// 	if ( east ) m_value = (byte) (m_value | EAST_JOIN);
// 	if ( south ) m_value = (byte) (m_value | SOUTH_JOIN);
// 	if ( west ) m_value = (byte) (m_value | WEST_JOIN);
//     }

    public boolean isLand()
    {
	// land is any empty land, leader, or non-blue tile
	// monuments and catastrophes lose their "landness" because
	// they can never again become empty land.
	return ( ( m_value & EMPTY_FIELD ) == EMPTY_LAND ||
		 ( m_value & TYPE ) == LEADER ||
		 ( (m_value & TYPE ) == TILE) && (getSphere() != BLUE) );
    }
    
    public boolean isWater()
    {
	// simmilar to land only blue tiles, no leaders, and empty water.
	return ( ( m_value & EMPTY_FIELD ) == EMPTY_WATER ||
		 ( (m_value & TYPE ) == TILE) && (getSphere() == BLUE) );
    }

    // returns one of the byte constants defined above 
    // e.g. MON, LEADER, EMPTY ...
    byte getType()
    {
	return (byte) (m_value & TYPE);
    }

    // set with one of the byte constants defined above.
    void setType( byte newType )
    {
	// set the bits to the new value
	// after first clearing them.
	m_value = (byte) ((m_value & ~TYPE ) | (newType & TYPE ));
    }

    // convenience methods.
    // should be inlined (or equivalent) for speed.
    int getDynasty()
    {
	return m_value << 30 >>> 30; 
    }    
    
    void setDynasty( int value )
    {
	// truncate all but three bits.
	int val3bit = value & 0x07;

	// clear the bits of the value.
	int tmp_value = m_value & ~DYN;

	// set the values to the new ones shifted into position.
	m_value = (byte) (tmp_value | val3bit);
    }

    int getSphere()
    {
	return m_value << 27 >>> 30;
    }

    void setSphere( Sphere s )
    {
	setSphere( getSphereInt( s ) );
    }

    void setSphere( int value )
    {
	// truncate all but two bits.
	int val2bit = value & 0x03;

	// clear the bits of the value
	int tmp_value = m_value & ~SPH;

	// set the values to the new ones shifted into position
	m_value = (byte) ( tmp_value  | (val2bit << 3 ));
    }

    // empty is not tile/leader/mon/cat.
    // in our case, EMPTY includes cat so we check for that.
    public boolean isEmpty()
    {
	return ( (m_value & EMPTY_FIELD) == EMPTY_WATER ||
		 (m_value & EMPTY_FIELD) == EMPTY_LAND );
    }

    public boolean hasTreasure()
    {
	return ( (m_value & HAS_TREAS) == HAS_TREAS );
    }

    public boolean hasMustTakeTreasure()
    {
	return ( (m_value & HAS_TREAS) == HAS_TREAS &&
		 (m_value & MUST_TAKE) == MUST_TAKE );
    }

    public void setTreasure( boolean value )
    {
	// this could potentially stomp stuff
	// if the space isn't valid to have a treasure on it.
	// i.e. a leader or a empty space

	int tmp_value = m_value;

	// set or clear the fields depending on the value
	if ( value )
	    // set the bits to one.
	    tmp_value = tmp_value | TREAS;
	else
	    // clear the bits to zero.
	    tmp_value = tmp_value & ~TREAS;

	// set the member variable
	m_value = (byte) tmp_value;
    }

    public void setMustTake( boolean value )
    {
	// this could potentially stomp stuff
	// if the space isn't valid to have a treasure on it.
	// i.e. a leader or a empty space

	int tmp_value = m_value;

	// set or clear the fields depending on the value
	if ( value )
	    // set the bits to one.
	    tmp_value = tmp_value | MUST_TAKE;
	else
	    // clear the bits to zero.
	    tmp_value = tmp_value & ~MUST_TAKE;

	// set the member variable
	m_value = (byte) tmp_value;
    }

    public Tile getTile()
    {
	if ( (m_value & TYPE)  != TILE )
	    return null;
	else
	    return new Tile( getIntSphere( getSphere() ));
    }

    public boolean hasTile()
    {
	return ( (m_value & TYPE ) == TILE );
    }

    public Leader getLeader()
    {
	if ( (m_value & TYPE)  != LEADER )
	    return null;
	else
	    return new Leader( getIntSphere( getSphere() ), getDynasty()); 
    }

    public boolean hasLeader()
    {
	return ( (m_value & TYPE) == LEADER );
    }

    public Monument getMonument()
    {
	if ( (m_value & TYPE)  != MON )
	    return null;
	else
	    return new Monument( getIntSphere( getSphere() ), 
				 getIntSphere( getDynasty()));
    }

    public boolean hasMonument()
    {
	return ( (m_value & TYPE) == MON );
    }

    public boolean hasCatastrophe()
    {
	return 	( (m_value & EMPTY_FIELD) == CAT );
    }

   public String getBitString()
   {
      String str = "";;
      
      // print out MSB to LSB of a byte.
      for ( int ii=0; ii<8; ii++) {
	 if ( ii == 4 ) str += " ";
	 //System.out.print( ( m_value << (24 + ii) >> 31  ) + " " );
	 str += ( ( m_value << (24 + ii) >>> 31  ) + "" );
      }
      return str;
   }

   public String toString()
   {
      return "Space2: "+ LocationImpl.formatLocation(this) + " bits " + getBitString();
   }

    public char toChar()
    {
       if ( isEmpty() )
	  {
	     if ( isLand() )
		return '.';
	     else
		return '~';
	  }
       else if ( hasCatastrophe() )
	  return 'x';
       else if ( hasLeader() )
	  return ("" + getDynasty() ).charAt( 0 );
       // maybe?  or maybe do something cool in Board2
       // where the red sphere is replaced with * or +
//        else if ( hasMonument() && hasTreasure() )
// 	  return 'N';
       else if ( hasMonument())
	  return 'M';
       else if ( hasMustTakeTreasure() )
	  return '*';
       else if ( hasTreasure() )
	  return '+';
       else if ( hasJoinTile() ) {
	  //System.out.println( "this produced join " + getBitString());
	  return 'J';
       }
       else if ( hasTile() )
	  return getIntSphere( getSphere() ).toChar();
       else {
	  System.out.println( "unknown space2 " + getBitString());
	  return '?';
       }
    }
}



