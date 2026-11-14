/* File: Player.java
 * Description:  Player in an E&T game.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

import com.tigrisgame.util.ArrayUtils;

import java.io.Serializable;

public class Player extends TileBag implements Serializable, Cloneable
{
   public static final int CATASTROPHES_PER_PLAYER=2;

   // dynasty
   public static final int
      INVALID= -1,
      BOW    = 0,
      OX     = 1,
      URN    = 2,
      LION   = 3;
   
   // the dynasty is exactly equal to the array index in game
   // the name is eye candy for any displays
   // and are not strictly needed by the rules
   private int dynasty;
   private String name = "-player name-";
   
   protected long clock;

    // game state fields.
    // protected so that HiddenPlayer objects can easily mutate as info becomes availible
    protected int catastrophes = CATASTROPHES_PER_PLAYER;
    protected Leader blackLeader, redLeader, blueLeader, greenLeader;
    protected int blackVP=0, redVP=0, blueVP=0, greenVP=0, m_treasures=0;

    public Player()
    {
	this.dynasty = INVALID;

	// an invalid player has no leaders, tiles, or catastrophes.
	catastrophes = 0;
    }

    public Player( int dynasty )
    {
	this.dynasty = dynasty;

	// by default a player starts with all his leaders.
	addLeader( Sphere.BLACK );
	addLeader( Sphere.RED );
	addLeader( Sphere.BLUE );
	addLeader( Sphere.GREEN );
    }

    public Player( int dynasty, String name )
    {
	this.dynasty = dynasty;
	this.name = name;

	// by default a player starts with all his leaders.
	addLeader( Sphere.BLACK );
	addLeader( Sphere.RED );
	addLeader( Sphere.BLUE );
	addLeader( Sphere.GREEN );
    }

    public Player( Player p )
    {
	init( p );
    }

    // method to reuse player objects
    public void init( Player p )
    {
	// init of TileBag superclass
	drawTiles( this );
	putTiles( p );

	// player fields init
	this.dynasty = p.dynasty;
	this.name = p.name;

	this.catastrophes = p.catastrophes;

	// shouldn't be any problem referencing the other guy's leaders,
	// since they are constant objects.
	// CAREFULL!
	// if the leaders are invalid, we won't know about it.
	this.blackLeader = p.blackLeader;
	this.redLeader = p.redLeader;
	this.blueLeader = p.blueLeader;
	this.greenLeader = p.greenLeader;

	// victory points
	this.blackVP = p.blackVP;
	this.redVP = p.redVP;
	this.blueVP = p.blueVP;
	this.greenVP = p.greenVP;
	this.m_treasures = p.m_treasures;
    }

    // system provided shallow copy.
    // good enough for player since all fields are primites 
    // or immutable object.
    public Object clone()
    {	
	// no need to try/catch since we know TileBag is cloneable.
	return super.clone();

//  	try
//  	    {
//  		return super.clone();
//  	    }
//  	catch (CloneNotSupportedException e)
//  	    {
//  		// this shouldn't happen, since we are Cloneable
//  		return null;
//  	    }
    }
    
   protected void setDynasty( int dyn )
   {
      this.dynasty = dyn;
   }
   
   public int getDynasty()
   {
      return dynasty;
   }

   public void setClock( long clk )
   {
      this.clock = clk;
   }

   public long getClock()
   {
      return clock;
   }

    public String getName()
    {
	return name;
    }

    public void setName( String name )
    {
	this.name = name;
    }

   public void addLeader( Sphere sp )
   {
      Leader leader = null;

      if (sp.isBlack())
	 blackLeader = new Leader( sp, dynasty );
      else if (sp.isRed())
	 redLeader = new Leader( sp, dynasty );
      else if (sp.isBlue())
	 blueLeader = new Leader( sp, dynasty );
      else if (sp.isGreen())
	 greenLeader = new Leader( sp, dynasty );
      else
	 System.out.println("Warning: Player.addLeader invalid Sphere type "+sp );
   }

    public void removeLeader( Sphere sp )
    {
	Leader leader = null;
	
	if (sp.isBlack())
	    blackLeader = null;
	else if (sp.isRed())
	    redLeader = null;
	else if (sp.isBlue())
	    blueLeader = null;
	else if (sp.isGreen())
	    greenLeader = null;
	else
	    System.out.println("Warning: Player.removeLeader invalid Sphere type "+sp );
    }

    public Leader getLeader( Sphere sp )
    {
	Leader leader = null;
	
	if (sp.isBlack())
	    leader = blackLeader;
	else if (sp.isRed())
	    leader = redLeader;
	else if (sp.isBlue())
	    leader = blueLeader;
	else if (sp.isGreen())
	    leader = greenLeader;
	else
	    System.out.println("Warning: Player.getLeader invalid Sphere type "+sp );
	
	return leader;
    }

    public int getNumLeaders()
    {
	int numLeaders = 0;

	if ( blackLeader != null ) numLeaders ++;
	if ( redLeader != null ) numLeaders ++;
	if ( blueLeader != null ) numLeaders ++;
	if ( greenLeader != null ) numLeaders ++;

	return numLeaders;
    }

    public void addVictoryPoints( Sphere sp, int num )
    {
	//System.out.println("**VP**");

	if (sp.isBlack())
	    blackVP += num;
	else if (sp.isRed())
	    redVP += num;
	else if (sp.isBlue())
	    blueVP += num;
	else if (sp.isGreen())
	    greenVP += num;
	else
	    System.out.println("Warning: Player.addVictoryPoints invalid Sphere type "+sp );
    }

    public void addVictoryPoint( Sphere sp )
    {
	//System.out.println("**VP**");

	if (sp.isBlack())
	    blackVP ++;
	else if (sp.isRed())
	    redVP ++;
	else if (sp.isBlue())
	    blueVP ++;
	else if (sp.isGreen())
	    greenVP ++;
	else
	    System.out.println("Warning: Player.addVictoryPoints invalid Sphere type "+sp );
    }

    public int getVP()
    {
	return getVictoryPoints();
    }

    public int getVictoryPoints()
    {
	return 	blackVP + redVP + blueVP + greenVP + m_treasures; 
    }

    public int getVP( Sphere sp )
    {
	return getVictoryPoints( sp );
    }

    public int getVictoryPoints( Sphere sp )
    {
	if (sp.isBlack())
	    return blackVP;
	else if (sp.isRed())
	    return redVP;
	else if (sp.isBlue())
	    return blueVP;
	else if (sp.isGreen())
	    return greenVP;
	else
	    {
		System.out.println("Warning: Player.getVictoryPoints " + 
				   "invalid Sphere type "+sp );
		return 0;
	    }
    }

    public void clearVictoryPoints()
    {
	blackVP = redVP = blueVP = greenVP = m_treasures = 0;
    }

   public void addTreasure()
   {
      m_treasures ++;
   }
   
   public int getNumTreasures()
   {
      return m_treasures;
   }

   public int [] getFinalVictoryPoints()
   {
      Sphere [] spheres = Sphere.getSpheres();
      int [] vps = new int [spheres.length];
      
      for (int ii=0; ii<spheres.length; ii++)
	 {
	    vps[ii] = getVictoryPoints( spheres[ii] );
	 }
      
      int [] assTreasure = assignTreasures( vps, m_treasures );
      
      int finalVps [] = new int [spheres.length];
      for (int ii=0; ii<spheres.length; ii++)
	 {
	    finalVps[ii] = vps[ii]+assTreasure[ii];		
	 }

      sortAscending( finalVps );
    
      // returns list with 0 index as lowest number.
      return finalVps;
   }

    class SphereIndexSorter implements ArrayUtils.Sorter
    {
	int[] values;
	public SphereIndexSorter( int[] values )
	{
	    this.values = values;
	}

	public boolean lteq( Object obj1, Object obj2 )
	{
	    int val1 = values[ ((Sphere)obj1).getColorCode() ];
	    int val2 = values[ ((Sphere)obj2).getColorCode() ];
	    return (val1 <= val2 );
	}
    }

    public Sphere [] getSpheresAscending()
    {
	Sphere spheres[] = Sphere.getSpheres();
	int sortVals[] = getTreasureAssignment();
	for ( int ss=0; ss<spheres.length; ss++) {
	    // sort primarily by VP. 
	    sortVals[ss] += getVP( spheres[ss] ) * 1000;
	}


	ArrayUtils.sortAscending( spheres, new SphereIndexSorter( sortVals ));

	return spheres;
    }

    public int [] getTreasureAssignment()
    {
	Sphere spheres[] = Sphere.getSpheres();
	int vps[] = new int [spheres.length];
	for (int ii=0; ii<spheres.length; ii++) {
	    vps[ii] = getVictoryPoints( spheres[ii] );
	}
      
	return assignTreasures( vps, m_treasures );
    }

   // a decent utility function.
   public static int [] assignTreasures( int [] spheres, int treasures )
   {
      int [] treasureDist = new int[ spheres.length ];
      while ( treasures > 0 ) {
	 int minNumber = Integer.MAX_VALUE;
	 int minIndex = -1;
	 // find the min.
	 for ( int ii=0; ii<spheres.length; ii++)
	    {
	       if ( spheres[ii] + treasureDist[ii] < minNumber ||
		    spheres[ii] + treasureDist[ii] == minNumber &&
		    treasureDist[ii] < treasureDist[minIndex] )
		  {
		     minIndex = ii;
		     minNumber = spheres[ii] + treasureDist[ii];
		  }
	    }
	 treasureDist[ minIndex ] ++;
	 treasures --;
      }
      
      return treasureDist;
   }

    // WISHLIST..
    // it would be nice if this sorting was moved into ArrayUtils.

    public static void sortAscending( int [] values )
    {
	for ( int j = 0; j < values.length; j++ )
	    {
		int big = j;
		for ( int k = 0; k < values.length; k++ )
  		    {
			if ( values[k] > values[big] )
			    {
				big = k;
			    }

			int tempInt = values[j];
  			values[j] = values[big];
  			values[big] = tempInt;
		    }
	    }
    }

    public int getNumCatastrophes()
    {
	return catastrophes;
    }

   protected void putCatastrophe()
   {
      catastrophes ++;
   }

    public void removeCatastrophe()
    {
	catastrophes --;
    }

   // Returns a string representation of the Player that is fully
   // able to be parsed back into a Player object by TigrisReader.
   // the format is 
   // dynasty/name/<TILES>/bvp/rvp/uvp/gvp/treasure/clock/
   // where dynasty is a single digit and 
   // TILES is the LetterString of the tiles in hand.
   // e.g.  "0/craig/BBRRRRXX/0/0/1/1/1/1200/"
   public String toString()
   {
      StringBuffer strbuf = new StringBuffer();

      // -- DYNASTY INFO ---
      strbuf.append( dynasty );
      strbuf.append("/");
      strbuf.append( name );
      strbuf.append("/");

      // --- LEADERS --- 
      if ( blackLeader != null )
	 strbuf.append("b");
      if ( redLeader != null )
	 strbuf.append("r");
      if ( blueLeader != null )
	 strbuf.append("u");
      if ( greenLeader != null )
	 strbuf.append("g");
      strbuf.append("/");

      // --- TILES ---
      strbuf.append( getLetterString() );
      for ( int ii=0; ii<getNumCatastrophes(); ii++)
	 strbuf.append("X");
      strbuf.append("/");

      // --- VICTORY POINTS ---
      strbuf.append( blackVP );
      strbuf.append("/");
      strbuf.append( redVP );
      strbuf.append("/");
      strbuf.append( blueVP );
      strbuf.append("/");
      strbuf.append( greenVP );
      strbuf.append("/");
      strbuf.append( m_treasures );
      strbuf.append("/");
      strbuf.append( clock );
      strbuf.append("/");

      return strbuf.toString();
    }
}
