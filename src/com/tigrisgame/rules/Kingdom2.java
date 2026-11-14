/* File: Kingdom2.java
 * Description:  Byte implemtation of Kingdom
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

/* NOTES:
 * Kingdom is a closely coupled extendion Board for special groups of spaces.
 * implementation Kingdom to be used with Board2.java
 * really just a reference back to board and a kingdom number.
 */

package com.tigrisgame.rules;

import java.util.Vector;

class Kingdom2 implements Kingdom
{
    int m_kingNum;

    Board2 m_board;
    Space2 tmp_space = new Space2(); // temparary so we only have to construct once.

    Kingdom2( int kingNum, Board2 board )
    {
	m_kingNum = kingNum;
	m_board = board;
    }

    public boolean isKingdomEqual( Kingdom k )
    {
	return (((Kingdom2)k).m_kingNum == m_kingNum );
    }

   public int getKingdomNum()
   {
      return m_kingNum;
   }

    // methods to access Spaces or other
    // contents of the kingdom.
    public Leader getLeader( Sphere s )
    {
	if ( s == null )
	    return null;

	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();

	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];
		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum &&
			 tmp_space.hasLeader() &&
			 tmp_space.getSphere() == Space2.getSphereInt( s ) )
			return tmp_space.getLeader();
		}
	return null;
    }

    public int getNumLeaders()
    {
	int numLeaders = 0;
	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();

	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];
		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum &&
			 tmp_space.hasLeader() )
			numLeaders ++;
		}
	return numLeaders;
    }

    // shortcut for external conflicts.
    // returning Leader [] might help in some implementations,
    // but it isn't used.
    public Sphere [] getConflictingSpheres( Kingdom k )
    {
	Sphere spheres [] = Sphere.getSpheres();
	boolean myLeader [] = new boolean[ spheres.length ];
	boolean yourLeader [] = new boolean[ spheres.length ];
	
	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();

	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];
		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum && 
			 tmp_space.hasLeader() )
			{
			    // i have the leader
			    myLeader[ tmp_space.getSphere() ] = true;
			}
		    else if ( m_board.m_kingdoms[rr][cc] == ((Kingdom2)k).m_kingNum &&
			      tmp_space.hasLeader() )
			{
			    // you have the leader.
			    yourLeader[ tmp_space.getSphere() ] = true;
			}
		}

	Vector conflictors = new Vector();
	for ( int ss=0; ss<spheres.length; ss++ )
	    {
		if ( myLeader[ss] && yourLeader[ss] )
		    conflictors.addElement( spheres[ss] );
	    }

	Sphere [] conflictorsArray = new Sphere[ conflictors.size() ];
	for ( int ii=0; ii<conflictorsArray.length; ii++ )
	    {
		conflictorsArray[ii] = (Sphere) conflictors.elementAt( ii );
	    }
	
	return conflictorsArray;
    }

    // used once now, but in general can be usefull.
    // identical to Board.getMonuments() PLUS an extra check for kingNum
    public Monument [] getMonuments()
    {
	Vector monVector = new Vector();

	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();

	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];

		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum && 
			 tmp_space.hasMonument() )
			{
			    // it has monument, check to make sure it is
			    // upper left so we don't add twice.
			    int s1 = tmp_space.getSphere();
			    int s2 = tmp_space.getDynasty();

			    boolean upperSide = true;
			    boolean leftSide = true;

			    if ( rr > 0 )
				{
				    tmp_space.m_value = m_board.m_spaces[rr-1][cc];
				    upperSide = (tmp_space.hasMonument() == false || tmp_space.getSphere() != s1 || tmp_space.getDynasty() != s2 );
				}
			    if ( cc > 0 )
				{
				    tmp_space.m_value = m_board.m_spaces[rr][cc-1];
				    leftSide = (tmp_space.hasMonument() == false || tmp_space.getSphere() != s1 || tmp_space.getDynasty() != s2 );
				}

			    if ( upperSide && leftSide )
				monVector.addElement( new Monument( new Sphere( Space2.getIntSphere( s1 )),
								    new Sphere( Space2.getIntSphere( s2 ) )));
			}
		}

	Monument [] monArray = new Monument[ monVector.size() ];
	for ( int ii=0; ii<monArray.length; ii++ )
	    {
		monArray[ii] = (Monument) monVector.elementAt( ii );
	    }
	
	return monArray;
    }

    // used to resolve external conflicts
    // when spaces need to be removed.
    public Space [] getTileSpaces( Sphere sp )
    {
	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();

	Vector spaceVector = new Vector();

	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];
		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum &&
			 tmp_space.hasTile() &&
			 tmp_space.getSphere() == Space2.getSphereInt( sp ) )
			spaceVector.addElement( m_board.getSpace( rr, cc ) );
		}

	Space [] spaceArray = new Space[ spaceVector.size() ];
	for ( int ii=0; ii<spaceArray.length; ii++ )
	    {
		spaceArray[ii] = (Space) spaceVector.elementAt( ii );
	    }

	//m_board.printAsciiMap();
	//m_board.printKingdoms();	

	return spaceArray;
    }

    public Space [] getSpaces()
    {
	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();
	
	Vector spaceVector = new Vector();
	
	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];
		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum )
			spaceVector.addElement( m_board.getSpace( rr, cc ) );
		}

	Space [] spaceArray = new Space[ spaceVector.size() ];
	spaceVector.copyInto( spaceArray );

	return spaceArray;
    }

    // necessary for treasure taking rules.
    // also, well placed in this interface.
    public int getNumTreasures()
    {
	int numTreasures = 0;
	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();

	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];
		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum &&
			 tmp_space.hasTreasure() )
			numTreasures ++;
		}
	
//  	if ( numTreasures > 1 )
//  	    {
//  		m_board.printAsciiMap();
//  		m_board.printKingdoms();	
//  		throw new RuntimeException(numTreasures + " treasures? in Kingdom " + m_kingNum);
//  	    }
	return numTreasures;
    }

    public Space [] getTreasureSpaces()
    {
	Vector spaceVector = new Vector();
	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();

	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];
		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum &&
			 tmp_space.hasTreasure() )
			spaceVector.addElement( m_board.getSpace( rr, cc ) );
		}

	Space [] spaceArray = new Space[ spaceVector.size() ];
	spaceVector.copyInto( spaceArray );

	return spaceArray;
    }

    // tests if a space is in a kingdom.
    public boolean hasSpace( Location s )
    {
	// check for null or out of bounds.
	// then check that the kingdom number is the same 
	// as this kingdom.
	return ( m_board.getSpace( s ) != null &&
		 m_board.m_kingdoms[ s.getRow() ][ s.getCol() ] == m_kingNum );
    }


    //implements TileHolder interface for calculting exteranl support...
    public boolean hasTiles()
    {
	// asymptotically the same efficiency but could be improved
	// by inlining and aborting.
	// not used very often though.
	return getNumTiles() != 0;
    }

    public int getNumTiles()
    {	
	int numTiles = 0;
	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();

	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];
		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum &&
			 tmp_space.hasTile() )
			numTiles ++;
		}
	return numTiles;
    }    

    public int getNumTiles( Sphere sp )
    {
	int numTiles = 0;
	int rows = m_board.getNumRows();
	int cols = m_board.getNumCols();

	for ( int rr=0; rr<rows; rr++)
	    for ( int cc=0; cc<cols; cc ++)
		{
		    tmp_space.m_value = m_board.m_spaces[rr][cc];
		    if ( m_board.m_kingdoms[rr][cc] == m_kingNum &&
			 tmp_space.hasTile() &&
			 tmp_space.getSphere() == Space2.getSphereInt( sp ) )
			numTiles ++;
		}
	return numTiles;
    }

    public int getNumTiles( Tile tl )
    {
	if ( tl.isHidden() )
	    return 0;
	else
	    return getNumTiles( (Sphere) tl ); 
    }
}


