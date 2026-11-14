/* File: Board2.java
 * Description:  Game state for E&T Board (implementation with binary packing)
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

// NOTE: most searches like getSpace( Leader) 
// are O(rows*cols) unless some form of hashing/caching is implemented.

package com.tigrisgame.rules;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Vector;

class Board2 implements Board, Serializable
{
   byte [][] m_spaces;
   byte [][] m_kingdoms;
   private JoinTile joinTile;
   private byte m_rows, m_cols;
   
   // a potentially open non-kingdom number.
   private byte m_nonKingNum = -1;
   //so we don't have to construct it for byte manipulation.
   private Space2 tmp_space = new Space2();
   public static final String DEFAULT_BOARD_STR = 
       "11/16/....~~~~~.+.~.../.*..~.......~..*/...~~+......~~../~~~~.........~~~/.............+~~/..............~./~~~~....+...~~~./.*.~~~~.....~.../......~~~~~~~.*./.....+........../..........+...../";

       //"11/16/....~~~~~...~.../....~.......~.../...~~.......~~../~~~~.........~~~/..............~~/..............~./~~~~....+...~~~./...~~~~.....~.../......~~~~~~~.../.....+....+...../.........++...../";

   public static Board defaultBoard;

   // serial id generated on 9/7/2002.
   // included both spaces and kings [][] byte arrays,
   // rows, cols, nonKingNum, and a tmpSpace.
   static final long serialVersionUID = -1378642411502255866L;

   public Board2()
   {
      init( loadDefaultBoard() );
   }

   public Board2(int rows, int cols)
   {
      m_spaces = new byte[ rows ][ cols ];
      m_kingdoms = new byte[ rows ][ cols ];
      
      this.m_rows = (byte) rows;
      this.m_cols = (byte) cols;
   }

//    public Board2( String mapfile )
//       throws IOException, FileNotFoundException
//    {

//       Board readBoard = reader.readBoard();
//       init( readBoard );
//    }

    public Board2( Board board )
    {
	init( board );
    }

    public Board2( Board2 board )
    {
	init( board );
    }

   protected static Board loadDefaultBoard()
   {
      if ( defaultBoard == null ) {
	 try {
	    TigrisReader reader = 
	       new TigrisReader( DEFAULT_BOARD_STR );	    
	    defaultBoard = reader.readBoard();
	    reader.close();
	 } catch (Exception ex) {
	    throw new RuntimeException("Unable to init board singleton, v.b.");
	 }
      }
      return defaultBoard;
   }

    // initialize from the general Board interface
    // this should only be called when reading a board from file
    // b/c the parser is written in Board1
    public void init( Board board )
    {
       // use our cool init if possible
       if ( board instanceof Board2 )
	  init( (Board2) board );
       
	this.m_cols = (byte) board.getNumCols();
	this.m_rows = (byte) board.getNumRows();
	m_spaces = new byte[ m_rows ][ m_cols ];
	m_kingdoms = new byte[ m_rows ][ m_cols ];
	JoinTile boardjt = board.getJoinTile();
	joinTile = (boardjt==null)?null: (JoinTile) boardjt.clone();

	for ( int rr=0; rr< m_rows; rr++)
	    for ( int cc=0; cc< m_cols; cc++)
		{
		    // probably could use a method to get value instead of a copy contructor.
		    Space2 s2 = new Space2( board.getSpace( rr, cc ) );
		    m_spaces[rr][cc] = s2.m_value;
		}
	updateKingdoms();
    }

    // initialize from the same implementation Board2.
    public void init( Board2 board )
    {
	JoinTile boardjt = board.getJoinTile();
	joinTile = (boardjt==null)?null: (JoinTile) boardjt.clone();
	//System.out.println(this + " copied join tile is " + joinTile );

	// check for null arrays,
	// check for same size boards.
	// ASSUME: that m_spaces and m_kingdoms are always
	// set to the same size at the same time.
	if ( m_spaces == null ||
	     m_cols != board.m_cols ||
	     m_rows != board.m_rows ) 
	    {
		this.m_cols = board.m_cols;
		this.m_rows = board.m_rows;

		m_spaces = new byte[ m_rows ][ m_cols ];
		m_kingdoms = new byte[ m_rows ][ m_cols ];
	    }

	// this is the fastest damn copy of info that java allows
	for ( int rr=0; rr< m_rows; rr++)
	    {
		System.arraycopy( board.m_spaces[rr], 0,
				  m_spaces[rr], 0, m_cols );
		System.arraycopy( board.m_kingdoms[rr], 0, 
				  m_kingdoms[rr], 0, m_cols );
	    }
    }

    public Board deepCopy()
    {
	//System.out.println("Board2.deepCopy");

	return new Board2( this );
    }

    boolean isKingNumOpen( byte num )
    {
	if ( num == 0 )
	    return false;

	for ( int rr=0; rr< m_rows; rr++)
	    for ( int cc=0; cc< m_cols; cc++)
		{
		    if ( m_kingdoms[rr][cc] == num )
			return false;
		}
	return true;
    }

    byte nextNonKingNum()
    {
	// starts negative.
	byte start = m_nonKingNum;

	// hmmm what is -1 % 3
	for ( byte ii = (byte) Math.min( -1, (start - 1) % 128  );
	      ii != start; 
	      ii = (byte) Math.min( -1, (ii - 1) % 128  ))
	    {
		//System.out.println("isKingNumOpen " + ii + isKingNumOpen( ii ) );	
		if ( isKingNumOpen( ii ) )
		    {
			m_nonKingNum = ii;
			return ii;
		    }
	    }
	
	System.out.println("Warning: ran out of non-kingdom numbers");
	return 0;
    }

    public void replaceKingdomNum( byte oldNum, byte newNum )
    {
	replaceKingdomNum( oldNum,newNum, m_rows-1 );
    }

    // replace kingdom numbers stopping at the row and col.
    void replaceKingdomNum( byte oldNum, byte newNum, int rowBreak )
    {
	// nothing needs changing.
	if ( newNum == oldNum )
	    return;

	for ( int rr=0; rr<= rowBreak; rr++)
	    for ( int cc=0; cc< m_cols; cc++)
		{
		    if ( m_kingdoms[rr][cc] == oldNum )
			m_kingdoms[rr][cc] = newNum;
		}
    }

    // **************************
    // Implement Board Interface
    // **************************
    public int getNumRows()
    {
	return m_rows;
    }

    public int getNumCols()
    {
	return m_cols;
    }    

    public void printAsciiMap()
    {
	DecimalFormat nf = new DecimalFormat();
	nf.setMinimumIntegerDigits( 2 );
	nf.setPositivePrefix( "" );

	System.out.println("Board2.printAsciiMap():");

	System.out.print("     ");
	for ( int cc=0; cc< m_cols; cc++ )
	    System.out.print( nf.format( cc ) + " ");
	System.out.println();

	System.out.print("     ");
	for ( int cc=0; cc< m_cols; cc++ )
	    System.out.print( "-- ");
	System.out.println();


	for ( int rr=0; rr< m_rows; rr++)
	    {
		System.out.print( nf.format( rr ) + " |");

		for ( int cc=0; cc< m_cols; cc++)
		    {
			tmp_space.m_value = m_spaces[rr][cc];
			if ( tmp_space.hasLeader() )
			    // use sphere's toChar in leader and dynasty
			    System.out.print( " " + tmp_space.getLeader().
					      getSphere().toChar() + 
					      tmp_space.getDynasty());
			else
			    System.out.print( "  " +tmp_space.toChar());
		    }
		System.out.println();
	    }
    }

    public void printKingdoms()
    {
	DecimalFormat nf = new DecimalFormat();
	nf.setMinimumIntegerDigits( 2 );
	nf.setPositivePrefix( "" );

	System.out.println("Board2.printKingdoms():");

	System.out.print("     ");
	for ( int cc=0; cc< m_cols; cc++ )
	    System.out.print( nf.format( cc ) + " ");
	System.out.println();

	System.out.print("     ");
	for ( int cc=0; cc< m_cols; cc++ )
	    System.out.print( "-- ");
	System.out.println();

	for ( int rr=0; rr< m_rows; rr++)
	    {
		// no space in front of the row header.
		nf.setPositivePrefix( "" );
		System.out.print( nf.format( rr ) + " |");

		// space positive and negative the same.
		nf.setPositivePrefix( " " );
		for ( int cc=0; cc< m_cols; cc++)
		    {
			System.out.print( nf.format( m_kingdoms[rr][cc] ));
		    }
		System.out.println();
	    }
    }

    // TODO?? how to handle error conditions for
    // 1. put tiles on monuments/cats
    // 2. leaders on water
    // 3. loc out of bounds
    public void putTile( Location loc, Sphere s )
    {
	// get our internal representation of the space's location.
	tmp_space.m_value = getSpace2( loc ).m_value;

	// modify bits in temp Space2 variable.
	// we must set equals TILE so we clear the treasure bit.
	tmp_space.m_value = Space2.TILE;
	tmp_space.setSphere( s );

	// set back bits to internal representation.
	m_spaces[ loc.getRow() ][ loc.getCol() ] = tmp_space.m_value;
    }

    public void putLeader( Location loc, Sphere s, int dynasty )
    {
	// get our internal representation of the space's location.
	tmp_space.m_value = getSpace2( loc ).m_value;

	// modify bits in temp Space2 variable.
	tmp_space.setType( Space2.LEADER );
	tmp_space.setSphere( s );
	tmp_space.setDynasty( dynasty );

	// set back bits to internal representation.
	m_spaces[ loc.getRow() ][ loc.getCol() ] = tmp_space.m_value;
    }

    public void putMonument( Location loc, Monument mon )
    {
	Space2 ul = (Space2) getSpace( loc );
	Space2 ur = (Space2) getEastSpace( ul );
	Space2 ll = (Space2) getSouthSpace( ul );
	Space2 lr = (Space2) getEastSpace( ll );

	putMonument( ul, mon.getFirstSphere(), mon.getSecondSphere() );
	putMonument( ur, mon.getFirstSphere(), mon.getSecondSphere() );
	putMonument( ll, mon.getFirstSphere(), mon.getSecondSphere() );
	putMonument( lr, mon.getFirstSphere(), mon.getSecondSphere() );
    }

    protected void putMonument( Location loc, Sphere s1, Sphere s2 )
    {
	// get our internal representation of the space's location.
	tmp_space.m_value = getSpace2( loc ).m_value;

	// modify bits in temp Space2 variable.
	tmp_space.setType( Space2.MON );
	tmp_space.setSphere( Space2.getSphereInt( s1 ) );
	tmp_space.setDynasty( Space2.getSphereInt( s2 ) );

	// set back bits to internal representation.
	m_spaces[ loc.getRow() ][ loc.getCol() ] = tmp_space.m_value;
    }

    public void putCatastrophe( Location loc )
    {
	// we can immediatly set the bits and blow away everything 
	// because once it becomes a catastrophe it loses all other info.
	m_spaces[ loc.getRow() ][ loc.getCol() ] = Space2.CAT;
    }

    public JoinTile getJoinTile()
    {
	//System.out.println(this + " join tile is " + this.joinTile );
	return joinTile;
    }

    public void putJoinTile( Location loc, Sphere sphere )
    {
	if ( loc == null ) {
	    this.joinTile = null;
	    return;
	}

	int row = loc.getRow();
	int col = loc.getCol();

	int kingNumN = getKingNum( row - 1, col );
	int kingNumS = getKingNum( row + 1, col );
	int kingNumE = getKingNum( row, col + 1 );
	int kingNumW = getKingNum( row, col - 1 );

	joinTile = new JoinTile( sphere, (byte) kingNumN, (byte) kingNumS, 
				 (byte) kingNumE, (byte) kingNumW );

	// get our internal representation of the space's location.
	tmp_space.m_value = Space2.JOIN;
	tmp_space.setSphere( sphere );
	
	m_spaces[ row ][ col ] = tmp_space.m_value;	
    }
    

   public void putTreasure( Location loc, boolean mustTake )
   {
      // get our internal representation of the space's location.
      tmp_space.m_value = getSpace2( loc ).m_value;

      // modify bits in temp Space2 variable.
      tmp_space.setTreasure( true );
      tmp_space.setMustTake( mustTake );
      
      // set back bits to internal representation.
      m_spaces[ loc.getRow() ][ loc.getCol() ] = tmp_space.m_value;
   }

    public void takeTreasure( Location loc )
    {
	// get our internal representation of the space's location.
	tmp_space.m_value = getSpace2( loc ).m_value;

	// modify bits in temp Space2 variable.
	tmp_space.setTreasure( false );

	// set back bits to internal representation.
	m_spaces[ loc.getRow() ][ loc.getCol() ] = tmp_space.m_value;
    }

   public void emptyLocation( Location loc ) { emptyLocation( loc, false );}

   public void emptyLocation( Location loc, boolean forceWater )
   {
      // get our internal representation of the space's location.
      tmp_space.m_value = getSpace2( loc ).m_value;
      
      // modify bits in temp Space2 variable.
      // we actually have to reconstruct the "landness" based on the original.
      // then we can blow away all the fields but the empty type
      // because there are no treasures on empty spots.
      if ( tmp_space.isWater() || forceWater )
	 tmp_space.m_value = Space2.EMPTY_WATER;
      else
	 // default illegal move (from cat, or mon) to land.
	 tmp_space.m_value = Space2.EMPTY_LAND;
      
      // set back bits to internal representation.
      m_spaces[ loc.getRow() ][ loc.getCol() ] = tmp_space.m_value;
   }
   
    public void updateKingdoms( Location sp )
    {
	// TODO... an more efficient version b/c we know only one 
	// kingdom effected.
	updateKingdoms();

	// ... it seems that this is also used for "picking up" a solitary leader
	// so we also have to handle splitering kingdoms

	/*
	int row = sp.getRow();
	int col = sp.getCol();
	byte groupAbove = row > 0 ? m_kingdoms[row-1][col] : 0;
	byte groupLeft  = col > 0 ? m_kingdoms[row][col-1] : 0;
	byte groupBelow = row < getNumRows() ? m_kingdoms[row+1][col] : 0;
	byte groupRight = col < getNumCols() ? m_kingdoms[row][col+1] : 0;

	if ( groupAbove > 0 ||
	     groupLeft > 0 ||
	     groupBelow > 0 ||
	     groupRight > 0 )
	    m_kingdoms[ row ][ col ] = Math.max( Math.max( groupAbove, groupLeft ), Math.max( groupBelow, groupRight ));
	return;
	*/
    }

    public void updateKingdoms()
    {
	// algorithm for grouping spaces into kingdoms (groups w/ leaders)
	// and non-kingdoms (groups w/o leaders)

	int nonKingNum = -1;

	for ( int rr=0; rr< m_rows; rr++)
	    for ( int cc=0; cc< m_cols; cc++) {

		// check if it has something to add to a kingdom.
		if ( (m_spaces[rr][cc] & Space2.KING_STUFF ) != 0 ) {

		    // check if it's neighbors are part of a group.
		    // NOTE: we only need to check the above and left
		    // neighbors because of the way we traverse the array.
		    byte groupAbove = rr > 0 ? m_kingdoms[rr-1][cc] : 0;
		    byte groupLeft = cc > 0 ? m_kingdoms[rr][cc-1] : 0;
		    
		    boolean leader = ( (m_spaces[rr][cc] & Space2.TYPE) 
				       == Space2.LEADER );
		    byte kingNum;
		    
		    // if a leader is not next to any kingdoms ->
		    // create a new kingdom number.
		    if ( leader && 
			 groupAbove <= 0 &&
			 groupLeft <= 0 ) {
			// merge above and below into a new number.
			// together, the sphere and dynasty bits are unqiue
			// for each leader, so that's a good choice
			// for a new number.
			// EXCEPT OF COURSE WHEN IT IS ZERO!! 
			// so we bitwise AND the value with TILE mask
			// 0100 0000 to force positive non-zerosmall_
			kingNum = (byte) (m_spaces[rr][cc] & 
					  ( Space2.SPH | Space2.DYN ) |
					  Space2.TILE );
		    }
		    // if nothing above and below but not a leader ->
		    // create a new non-kingdom number.
		    else if ( groupAbove == 0 &&
			      groupLeft == 0 ) {
			// need a new non-kingdom number.
			// since we are starting everthing from scratch
			// we can use a simple decrementing number until
			// we use 128 of them, then we have to check for gaps.
			if ( nonKingNum > -128 )
			    kingNum = (byte) (-- nonKingNum);
			else
			    kingNum = nextNonKingNum();
		    }
		    // if group above is a kingdom OR
		    // no left kingdom ->
		    // use the kingdom number from above.
		    else if ( groupAbove  > 0 ||
			      groupLeft == 0 ) {
			kingNum = groupAbove;
		    }
			// otherwise use kingdom left.
		    else {
			kingNum = groupLeft;
		    }
			
		    // set the space.
		    m_kingdoms[rr][cc] = kingNum;
			
		    // merge in above and below if they exist.
		    if ( groupAbove != 0 )
			replaceKingdomNum( groupAbove, kingNum, rr );
		    if ( groupLeft != 0 )
			replaceKingdomNum( groupLeft, kingNum, rr );
		}
		else {
		    // clear out any old spaces that don't have anything.
		    m_kingdoms[rr][cc] = 0;
		}
	    }

	updateKingdomsJoinTilePass();
    }

    // do a 2nd pass over the kingdoms to update based on the 
    // the connectivity of the join tile.
    public void updateKingdomsJoinTilePass()
    {
	if ( joinTile == null )
	    return;

// 	int rr = joinSpace.getRow();
// 	int cc = joinSpace.getCol();

// 	// if at least one more row or column grab the kingdom number.
// 	// otherwise zero will be the "empty" value.
// 	byte groupAbove = rr > 0 ? m_kingdoms[rr-1][cc] : 0;
// 	byte groupLeft = cc > 0 ? m_kingdoms[rr][cc-1] : 0;	
// 	byte groupRight = rr < (m_rows-1) ? m_kingdoms[rr+1][cc] : 0;	
// 	byte groupBelow = cc < (m_cols-1) ? m_kingdoms[rr][cc+1] : 0;	

// 	byte firstKing = 0;
// 	byte secondKing = 0;

// 	// find out who is the first kingdom number.
// 	if ( groupAbove > 0 )
// 	    firstKing = groupAbove;
// 	else if ( groupLeft > 0 && joinTile.isJoinNorthWest() )
// 	    firstKing = groupLeft;
// 	else if ( groupRight > 0 && joinTile.isJoinNorthEast() )
// 	    firstKing = groupRight;
// 	else if ( groupBelow > 0 && joinTile.isJoinNorthSouth() )
// 	    firstKing = groupBelow;
// 	// else -- ok to have no first king, 
// 	// the conflict could be resolved.

// 	// ok, now make all tiles that are connected to the 
// 	// north tile the same.
// 	if ( groupAbove != 0 )
// 	    replaceKingdomNumber( groupAbove, firstKing );
// 	if ( groupLeft != 0 && joinTile.isJoinNorthWest() )
// 	    replaceKingdomNumber( groupLeft, firstKing );
// 	if ( groupRight != 0 && joinTile.isJoinNorthEast() )
// 	    replaceKingdomNumber( groupRight, firstKing );
// 	if ( groupBelow != 0 && joinTile.isJoinNorthSouth() )
// 	    replaceKingdomNumber( groupBelow, firstKing );
		
// 	// any kingdom left in the second kingdom, let's find it.
// 	if ( groupLeft > 0 && groupLeft != firstKing )
// 	    secondKing = groupLeft;
// 	else if ( groupRight > 0 && groupRight != firstKing ) 
// 	    secondKing = groupRight;
// 	else if ( groupBelow > 0 && groupBelow != firstKing ) 
// 	    secondKing = groupBelow;

// 	// ok, now make all tiles that are connected to the 
// 	// north tile the same.
// 	if ( groupLeft != 0 && !joinTile.isJoinNorthWest() )
// 	    replaceKingdomNumber( groupLeft, secondKing );
// 	if ( groupRight != 0 && !joinTile.isJoinNorthEast() )
// 	    replaceKingdomNumber( groupRight, secondKing );
// 	if ( groupBelow != 0 && !joinTile.isJoinNorthSouth() )
// 	    replaceKingdomNumber( groupBelow, secondKing );

// 	// should be good now.  a rather expensive and complex operation
// 	// for a very subtle and infrequent rules change.
    }
	


    // decent amount of error checking.
    // returns null if invalid.
    public Space getSpace( int row, int col )
    {
	if ( row < 0 ||
	     col < 0 ||
	     row >= m_rows ||
	     col >= m_cols )
	    return null;
	else
	    {
		return new Space2( m_spaces[row][col], row, col );
	    }
    }

    public int getKingNum( int row, int col )
    {
	if ( row < 0 ||
	     col < 0 ||
	     row >= m_rows ||
	     col >= m_cols )
	    return 0;
	else
	    return m_kingdoms[row][col];
    }

    // decent amount of error checking.
    // returns null if invalid.
    public Space getSpace( Location loc )
    {
	if ( loc == null )
	    return null;
	else
	    return getSpace( loc.getRow(), loc.getCol() );
    }

    // get the internal type for convenience.
    Space2 getSpace2( Location s )
    {
	if ( s == null )
	    return null;

	int row = s.getRow();
	int col = s.getCol();

	if ( row < 0 ||
	     col < 0 ||
	     row >= m_rows ||
	     col >= m_cols ) {
	   System.out.println("board2 " + m_rows + "x" + m_cols + 
			      " requested oob lob: " + s );
	   return null;
	}
	else
	    {
		return new Space2( m_spaces[row][col], row, col );
	    }
    }


    public Space getSpace( Leader l )
    {
	if ( l == null )
	    return null;

	for ( int rr=0; rr<m_rows; rr++)
	    for ( int cc=0; cc<m_cols; cc ++)
		{
		    tmp_space.m_value = m_spaces[rr][cc];
		    if ( tmp_space.hasLeader() &&
			 tmp_space.getDynasty() == l.getDynasty() &&
			 tmp_space.getSphere() == 
			 Space2.getSphereInt( l.getSphere() ) )
			return getSpace( rr, cc );
		}
	return null;
    }

    // returns a Space array in spheres for a given dynasty.  
    // leaders not on the board will have null in the appropriate index.
    // better than getSpace( Leader l ) if you are searching for all
    // spheres since it will only loop O(spaces) 1 times instead of 4.
    public Space [] getLeaderSpaces( int dynasty, Sphere [] spheres )
    {
	Space [] leaderSpaces = new Space[ spheres.length ];

	for ( int rr=0; rr<m_rows; rr++) {
	    for ( int cc=0; cc<m_cols; cc ++) {
		tmp_space.m_value = m_spaces[rr][cc];
		if ( tmp_space.hasLeader() &&
		     tmp_space.getDynasty() == dynasty )
		    for ( int ss=0; ss<spheres.length; ss++ ) {
			if ( tmp_space.getSphere() ==
			     spheres[ss].getColorCode() )
			    leaderSpaces[ tmp_space.getSphere() ] =
				getSpace( rr, cc );
		    }
	    }
	}
	return leaderSpaces;
    }

   // Reimplemented this method to use the 2x2 monument trick.
   // There is some extra work pinpointing the UL corner once we get a hit.
   // Overall, the running time decreased by 33%, 
   // which is a respectable improvement.
   public Space getUpperLeftSpace( Monument m )
   {
      if ( m == null ) {
	 System.out.println("warning, asked upper left of null");
	 return null;
      }
      
      Monument tmpMonument = new Monument( null, null );
      Space theSpace = null;

      // an implementation to take advantage.
      // or monuments 2x2 size.
      loop:
      for ( int rr=0; rr<m_rows; rr+=2)
	 for ( int cc=0; cc<m_cols; cc+=2) {
	    tmp_space.m_value = m_spaces[rr][cc];		    
	    
	    // has monument.
	    if ( tmp_space.hasMonument() ) {
	       
	       int s1 = tmp_space.getSphere();
	       int s2 = tmp_space.getDynasty();
	       tmpMonument.firstSphere = Space2.getIntSphere( s1 );
	       tmpMonument.secondSphere = Space2.getIntSphere( s2 );

	       if ( tmpMonument.equals( m ) ) {

// 		  System.out.println("hit at " + rr + ", " + cc );

		  // a hit! now find what part of the monument we hit
		  boolean isNorth;
		  boolean isWest;
		  
		  Location loc = new LocationImpl( rr, cc );
		  Space2 northSpace = (Space2)getNorthSpace( loc );
		  Space2 westSpace = (Space2)getWestSpace( loc );
		  
		  isNorth = ( northSpace != null &&
			      northSpace.hasMonument() &&
			      monumentMatches( tmp_space, northSpace ));
		  isWest = ( westSpace != null &&
			     westSpace.hasMonument() &&
			     monumentMatches( tmp_space, westSpace ));
		  
		  if ( isNorth ) {
		     if ( isWest )
			// space is NW. 
			theSpace = getWestSpace( northSpace );
		     else // space is N.
			theSpace = northSpace;
		  }
		  else {
		     if ( isWest )
			// space is W.
			theSpace = westSpace;
		     else 
			// space is center.
			theSpace = getSpace( rr, cc );
		  }

// 		  System.out.println("north west " + northSpace + ", " + westSpace );		  
// 		  System.out.println("north? west? " + isNorth + ", " + isWest );

		  break loop;
	       } // if ( tmpMonument.equals( m )
	    } // for cols
	 } // for rows
      
      // temp for debug.
      if ( theSpace == null ) {
	 System.out.println("UPPER LEFT SPACE FOR " + m + " is " + theSpace );
      }

      return theSpace;
   }

   private boolean monumentMatches( Space2 s1, Space2 s2 )
   {
      return 
	 (s1.getSphere() == s2.getSphere() ||
	  s1.getSphere() == s2.getDynasty() ) &&
	 (s1.getDynasty() == s2.getSphere() ||
	  s1.getDynasty() == s2.getDynasty() );
   }

    // return only "kingdoms" with leaders.
    public Kingdom getKingdom( Location s )
    {
	Space sGood = getSpace( s ); // make sure okay.

	if ( sGood != null &&
	     m_kingdoms[ sGood.getRow() ][ sGood.getCol() ] > 0 )
	    {
		return new Kingdom2( m_kingdoms[ sGood.getRow() ][ sGood.getCol() ], this );
	    }
	return null;
    }

   public int getKingdomNum( Location s )
   {
      Space sGood = getSpace( s ); // make sure okay.
      if ( sGood != null &&
	   m_kingdoms[ sGood.getRow() ][ sGood.getCol() ] > 0 )
	 {
	    return m_kingdoms[ sGood.getRow() ][ sGood.getCol() ];
	 }
      return 0;
   }

    public Kingdom getKingdom( Leader l )
    {
	Space sGood = getSpace( l ); // make sure okay.

	if ( sGood != null &&
	     m_kingdoms[ sGood.getRow() ][ sGood.getCol() ] > 0 )
	    {
		return new Kingdom2( m_kingdoms[ sGood.getRow() ][ sGood.getCol() ], this );
	    }
	return null;
    }


    public Space [] getAdjacentSpaces( Location s )
    {
	Space sGood = getSpace( s ); // make sure okay.
	
	if ( sGood == null )
	    return new Space[0];

	int row = sGood.getRow();
	int col = sGood.getCol();

	Vector adjacentVect = new Vector();

	if ( row > 0 )
	    adjacentVect.addElement( new Space2( m_spaces[row-1][col], row -1, col ) );
	if ( row < m_rows - 1 )
	    adjacentVect.addElement( new Space2( m_spaces[row+1][col], row +1, col ));
	if ( col > 0 )
	    adjacentVect.addElement( new Space2( m_spaces[row][col-1], row, col - 1 ));
	if ( col < m_cols - 1 )
	    adjacentVect.addElement( new Space2( m_spaces[row][col+1], row, col + 1 ));

	Space [] adjacent = new Space[ adjacentVect.size() ];
	for ( int ii=0; ii<adjacentVect.size(); ii++)
	    {
		adjacent[ii] = (Space) adjacentVect.elementAt( ii );
	    }
	return adjacent;
    }

    // used once by ai.
    // hmm.. this could be a little bit hard.
    public Space [] getAdjacentSpaces( Kingdom king )
    {
	if ( king == null )
	    return new Space[0];

	Kingdom2 k2 = (Kingdom2) king;
	Vector adjacentVect = new Vector();

	for ( int rr=0; rr<m_rows; rr++)
	    for ( int cc=0; cc<m_cols; cc ++)
		{
		    if ( isLocationAdjacentKingdom( rr, cc, k2.m_kingNum ))
			adjacentVect.addElement( getSpace( rr, cc ) );
		}

	Space [] adjacent = new Space[ adjacentVect.size() ];
	for ( int ii=0; ii<adjacentVect.size(); ii++)
	    {
		adjacent[ii] = (Space) adjacentVect.elementAt( ii );
	    }
	return adjacent;
    }

    // a helper method for getAdjacentSpaces
    private boolean isLocationAdjacentKingdom( int row, int col, int kingNum )
    {
	return ( ( row > 0 && kingNum == m_kingdoms[row-1][col] ) ||
		 ( row < m_rows - 1 && kingNum == m_kingdoms[row+1][col] ) ||
		 ( col > 0  && kingNum ==  m_kingdoms[row][col-1] ) ||
		 ( col < m_cols - 1 && kingNum ==  m_kingdoms[row][col+1] ));
    }

    public Space getEastSpace( Location s )
    {
	if ( s == null ) 
	    return null;
	else
	    return getSpace( s.getRow(), s.getCol()+1 );
    }

    public Space getSouthSpace( Location s )
    {
	if ( s == null )
	    return null;
	else
	    return getSpace( s.getRow()+1, s.getCol() );
    }

    public Space getWestSpace( Location s )
    {
	if ( s == null )
	    return null;
	else
	    return getSpace( s.getRow(), s.getCol()-1 );
    }

   public Space getNorthSpace( Location s )
   {
      if ( s == null )
	 return null;
      else {
	 return getSpace( s.getRow()-1, s.getCol() );
      }
   }

    public int getNumCatastrophes()
    {
	int cats = 0;
	for ( int rr=0; rr<m_rows; rr++)
	    for ( int cc=0; cc<m_cols; cc ++)
		{
		    tmp_space.m_value = m_spaces[rr][cc];
		    if ( tmp_space.hasCatastrophe() )
			cats ++;
		}
	return cats;
    }
    
    public int getNumTreasures()
    {
	int treas = 0;

	for ( int rr=0; rr<m_rows; rr++)
	    for ( int cc=0; cc<m_cols; cc ++)
		{
		    tmp_space.m_value = m_spaces[rr][cc];
		    if ( tmp_space.hasTreasure() )
			treas ++;
		}
	return treas;
    }

    public int getNumAdjKingdoms( Location s )
    {
	int row = s.getRow();
	int col = s.getCol();

	// there are up to 4 adjacent kingdoms,
	// order and names don't matter.
	int kingNumN = getKingNum( row - 1, col );
	int kingNumS = getKingNum( row + 1, col );
	int kingNumE = getKingNum( row, col + 1 );
	int kingNumW = getKingNum( row, col - 1 );

	int adjCount = 0;

	if ( kingNumN > 0 )
	    adjCount ++;

	if ( kingNumS > 0 &&
	     kingNumS != kingNumN )
	    adjCount ++;

	if ( kingNumE > 0 && 
	     kingNumE != kingNumN &&
	     kingNumE != kingNumS )
	    adjCount ++;

	if ( kingNumW > 0 && 
	     kingNumW != kingNumN &&
	     kingNumW != kingNumS &&
	     kingNumW != kingNumE )
	    adjCount ++;

	return adjCount;
    }

    public Kingdom [] getAdjacentKingdoms( Location s )
    {
	// getKingNum will recover any out of bounds errors on 
	// the row and col below.
// 	Space sGood = getSpace( s ); // make sure okay.
        if ( s == null )
	    return new Kingdom[0];

	int row = s.getRow();
	int col = s.getCol();

	Vector adjacentList = null;

	// replacing with sequence of kingNum*
	// this way, we won't have to init Integer or Vector objects.
	// Vector adjacentKingNums = new Vector();
	// Integer kingNum;
	int kingNumN = getKingNum( row - 1, col );
	int kingNumS = getKingNum( row + 1, col );
	int kingNumE = getKingNum( row, col + 1 );
	int kingNumW = getKingNum( row, col - 1 );

	int adjCount = 0;

	if ( kingNumN > 0 ||
	     kingNumS > 0 ||
	     kingNumE > 0 ||
	     kingNumW > 0 ) {

	    // only init the ArrayList if we have at least one kingdom.
	    adjacentList = new Vector();

	    if ( kingNumN > 0 )
		adjacentList.addElement( new Kingdom2( kingNumN, this ));
	    
	    if ( kingNumS > 0 &&
		 kingNumS != kingNumN )
		adjacentList.addElement( new Kingdom2( kingNumS, this ));
	    
	    if ( kingNumE > 0 && 
		 kingNumE != kingNumN &&
		 kingNumE != kingNumS )
		adjacentList.addElement( new Kingdom2( kingNumE, this ));
	    
	    if ( kingNumW > 0 && 
		 kingNumW != kingNumN &&
		 kingNumW != kingNumS &&
		 kingNumW != kingNumE )
		adjacentList.addElement( new Kingdom2( kingNumW, this ));
	} else {
	    return new Kingdom[0];
	}

	Kingdom [] adjacent = new Kingdom[ adjacentList.size() ];
	for ( int ii=0; ii<adjacentList.size(); ii++)
	    {
		adjacent[ii] = (Kingdom) adjacentList.elementAt( ii );
	    }
	return adjacent;
    }


   // determine if the space is a break point
   public boolean isLocalBreak( Location sp )
   {
      return false;
   }
   
    public Kingdom [] getKingdoms()
    {
	Vector kingsVect = new Vector();
	Vector kingsKingNums = new Vector();
	Integer kingNum;

	for ( int rr=0; rr<m_rows; rr++)
	    for ( int cc=0; cc<m_cols; cc ++)
		{
		    kingNum = new Integer( getKingNum(rr,cc) );
		    if  ( kingNum.intValue() > 0 &&
			  ! kingsKingNums.contains( kingNum ) )
			{
			    kingsKingNums.addElement( kingNum );
			    kingsVect.addElement( new Kingdom2( m_kingdoms[ rr ][ cc ], this ));
			}
		}

	Kingdom [] kings = new Kingdom[ kingsVect.size() ];
	for ( int ii=0; ii<kingsVect.size(); ii++)
	    {
		kings[ii] = (Kingdom) kingsVect.elementAt( ii );
	    }
	return kings;
    }

    // CM - optimized on 9/7/2002 to only look at *every other*
    // row and col, because of this, we do'nt need to check for upper left
    // since each mon is 2x2 and will show up once this way.
    public Monument [] getMonuments()
    {
	Vector monVector = new Vector( 6 ); // max of 6 mons.

	for ( int rr=0; rr<m_rows; rr+=2)
	    for ( int cc=0; cc<m_cols; cc+=2) {
		tmp_space.m_value = m_spaces[rr][cc];
		
		if ( tmp_space.hasMonument() ) {
		    // it has monument, check to make sure it is
		    // upper left so we don't add twice.
		    int s1 = tmp_space.getSphere();
		    int s2 = tmp_space.getDynasty();
		    
// 		    boolean upperSide = true;
// 		    boolean leftSide = true;
		    
// 		    if ( rr > 0 )
// 			{
// 			    tmp_space.m_value = m_spaces[rr-1][cc];
// 			    upperSide = (tmp_space.hasMonument() == false || 
// 					 tmp_space.getSphere() != s1 || 
// 					 tmp_space.getDynasty() != s2 );
// 			}
// 		    if ( cc > 0 )
// 			{
// 			    tmp_space.m_value = m_spaces[rr][cc-1];
// 			    leftSide = (tmp_space.hasMonument() == false || 
// 					tmp_space.getSphere() != s1 || 
// 					tmp_space.getDynasty() != s2 );
// 			}
		    
// 		    if ( upperSide && leftSide )

		    Monument mon = new Monument( Space2.getIntSphere( s1 ),
						 Space2.getIntSphere( s2 ));
		    monVector.addElement( mon );
		}
	    }
    
	Monument [] monArray = new Monument[ monVector.size() ];
	for ( int ii=0; ii<monArray.length; ii++ )
	    {
		monArray[ii] = (Monument) monVector.elementAt( ii );
	    }
	
	return monArray;
    }

    // check 4 locations
    public boolean isInTileSquare( Location loc )
    {
	int row = loc.getRow();
	int col = loc.getCol();
	return ( isTileSquare( getSpace( row-1, col -1 )) ||
		 isTileSquare( getSpace( row-1, col )) ||
		 isTileSquare( getSpace( row, col-1 )) ||
		 isTileSquare( getSpace( row, col )) );
    }

    // check just UL.
    public boolean isTileSquare( Location upper_left )
    {
	Space sGood = getSpace( upper_left ); // make sure okay.

	if ( sGood == null )
	    return false;

	int row = sGood.getRow();
	int col = sGood.getCol();

	// it is all the way on the side, it can't 
	// be the upper left of any 2x2 sqaure.
	if ( row >= m_rows - 1 ||
	     col >= m_cols - 1)
	    return false;

	tmp_space.m_value = m_spaces[ row ][ col ];
	int sphereNum = tmp_space.getSphere();
	boolean allMatch = true;

	if ( ! tmp_space.hasTile() ||
	     tmp_space.getSphere() != sphereNum )
	    allMatch = false;

	tmp_space.m_value = m_spaces[ row + 1 ][ col ];
	if ( ! tmp_space.hasTile() ||
	     tmp_space.getSphere() != sphereNum )
	    allMatch = false;

	tmp_space.m_value = m_spaces[ row ][ col + 1 ];
	if ( ! tmp_space.hasTile() ||
	     tmp_space.getSphere() != sphereNum )
	    allMatch = false;
	
	tmp_space.m_value = m_spaces[ row + 1 ][ col +1 ];
	if ( ! tmp_space.hasTile() ||
	     tmp_space.getSphere() != sphereNum )
	    allMatch = false;

	return allMatch;
    }

    public boolean hasAdjacentLeader( Location sp )
    {
	int row = sp.getRow();
	int col = sp.getCol();

	if ( row > 0 )
	    {
		tmp_space.m_value = m_spaces[row-1][col];
		if ( tmp_space.hasLeader() )
		    return true;
	    }
	if ( row < m_rows - 1 )
	    {
		tmp_space.m_value = m_spaces[row+1][col];
		if ( tmp_space.hasLeader() )
		    return true;
	    }
	if ( col > 0 )
	    {
		tmp_space.m_value = m_spaces[row][col-1];
		if ( tmp_space.hasLeader() )
		    return true;
	    }
	if ( col < m_cols - 1 )
	    {
		tmp_space.m_value = m_spaces[row][col+1];
		if ( tmp_space.hasLeader() )
		    return true;
	    }
	return false;
    }

    public boolean isKingdomEqual( Location sp1, Location sp2 )
    {
	int kingNum1 = getKingNum( sp1.getRow(), sp1.getCol() );
	int kingNum2 = getKingNum( sp2.getRow(), sp2.getCol() );

	return ( kingNum1 == kingNum2 );
    }
    
    public int getNumAdjacentTiles( Location space, Sphere sphere )
    {
	if ( space == null ||
	     sphere == null )
	    return 0;

	int row = space.getRow();
	int col = space.getCol();
	int numTiles = 0;
	int sphereVal = Space2.getSphereInt( sphere );

	if ( row > 0 )
	    {
		tmp_space.m_value = m_spaces[row-1][col];
		if ( tmp_space.hasTile() &&
		     tmp_space.getSphere() == sphereVal )
		    numTiles ++;
	    }
	if ( row < m_rows - 1 )
	    {
		tmp_space.m_value = m_spaces[row+1][col];
		if ( tmp_space.hasTile() &&
		     tmp_space.getSphere() == sphereVal )
		    numTiles ++;
	    }
	if ( col > 0 )
	    {
		tmp_space.m_value = m_spaces[row][col-1];
		if ( tmp_space.hasTile() &&
		     tmp_space.getSphere() == sphereVal )
		    numTiles ++;
	    }
	if ( col < m_cols- 1 )
	    {
		tmp_space.m_value = m_spaces[row][col+1];
		if ( tmp_space.hasTile() &&
		     tmp_space.getSphere() == sphereVal )
		    numTiles ++;
	    }
	//System.out.println("getAdjacentTiles " + row + "," + col + " " + sphere + " " + numTiles );

	return numTiles;
    }

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
	for ( int rr=0; rr<m_rows; rr++)
	    for ( int cc=0; cc<m_cols; cc ++)
		{
		    tmp_space.m_value = m_spaces[rr][cc];
		    if ( tmp_space.hasTile() )
			numTiles ++;
		}
	return numTiles;
    }

    public int getNumTiles( Sphere sp )
    {
	int numTiles = 0;
	for ( int rr=0; rr<m_rows; rr++)
	    for ( int cc=0; cc<m_cols; cc ++)
		{
		    tmp_space.m_value = m_spaces[rr][cc];
		    if ( tmp_space.hasTile() &&
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

    public int getNumUnknown()
    {
	return 0;
    }

    public int getDistance( Location s1, Location s2 )
    {
	int xDis = Math.abs( s1.getRow() - s2.getRow() );
	int yDis = Math.abs( s1.getCol() - s2.getCol() );
	
	return xDis + yDis;
    }

    public int getDistance( Location s1, Kingdom k2 )
    {
	// todo.
	return 0;
    }

    public int getDistance( Kingdom k1, Kingdom k2 )
    {
	// todo. 
	return 0;
    }

    public Space [] getTreasureSpaces()
    {
	Vector spaceVector = new Vector();

	for ( int rr=0; rr<m_rows; rr++)
	    for ( int cc=0; cc<m_cols; cc ++)
		{
		    tmp_space.m_value = m_spaces[rr][cc];
		    if ( tmp_space.hasTreasure() )
			spaceVector.addElement( getSpace( rr, cc ) );
		}

	Space [] spaceArray = new Space[ spaceVector.size() ];
	for ( int ii=0; ii<spaceArray.length; ii++ )
	    {
		spaceArray[ii] = (Space) spaceVector.elementAt( ii );
	    }
	return spaceArray;
    }
    
    // "kingdoms" without leaders.
    public Kingdom [] getNonKingdoms()
    {
	Vector kingsVect = new Vector();
	Vector kingsKingNums = new Vector();
	Integer kingNum;

	for ( int rr=0; rr<m_rows; rr++)
	    for ( int cc=0; cc<m_cols; cc ++)
		{
		    kingNum = new Integer( getKingNum(rr,cc) );
		    if  ( kingNum.intValue() < 0 &&
			  ! kingsKingNums.contains( kingNum ) )
			{
			    kingsKingNums.addElement( kingNum );
			    kingsVect.addElement( new Kingdom2( m_kingdoms[ rr ][ cc ], this ));
			}
		}

	Kingdom [] kings = new Kingdom[ kingsVect.size() ];
	for ( int ii=0; ii<kingsVect.size(); ii++)
	    {
		kings[ii] = (Kingdom) kingsVect.elementAt( ii );
	    }
	return kings;
    }

   public String toString() {
      StringBuffer strbuf = new StringBuffer(m_rows*(m_cols+1)*8);
      strbuf.append( m_rows );
      strbuf.append( "/" );
      strbuf.append( m_cols );
      strbuf.append( "/" );
      Sphere []spheres = Sphere.getSpheres();
      for ( int rr=0; rr<m_rows; rr++) {
	 for ( int cc=0; cc<m_cols; cc++) {
	    tmp_space.m_value = m_spaces[rr][cc];
	    strbuf.append( tmp_space.toChar() );

	    // HANDLE special multi char cases.
	    // leader
	    if (tmp_space.hasLeader() )
	       strbuf.append( spheres[ tmp_space.getSphere() ].toChar() );

	    // monument, possibly with treasure. 
	    if ( tmp_space.hasMonument()) {

	       char s1char = spheres[ tmp_space.getSphere() ].toChar();
	       char s2char = spheres[ tmp_space.getDynasty() ].toChar();
	       // ensure red first (so we know who to replace w/ treasure
	       if ( s2char == Sphere.RED_CHAR ) {
		  s2char = s1char; s1char = Sphere.RED_CHAR;
	       }

	       // check each of the two positions. the place of the +,*
	       // will determine where the treasure is found int he monument.
	       if ( tmp_space.hasTreasure() ) {
		  strbuf.append( tmp_space.hasMustTakeTreasure()?
				 "*":"+" );
		  strbuf.append( s2char );
	       } 
	       else if ( getSpace( rr, cc+1 ).hasTreasure() ) {
		  strbuf.append( s2char );
		  strbuf.append( getSpace(rr,cc+1).hasMustTakeTreasure()?
				 "*":"+" );
	       } else {
		  strbuf.append( s1char );
		  strbuf.append( s2char );
	       }
	       cc++; // skip next space (2nd half of monument)
	    }
	    // join tile.
	    if ( tmp_space.hasJoinTile() ) {
	       strbuf.append( spheres[ tmp_space.getSphere() ].toChar() );
	    }
	 }
	 strbuf.append("/");
      }
      return strbuf.toString();
   }
}


