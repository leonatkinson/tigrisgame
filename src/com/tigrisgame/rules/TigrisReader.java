/* File: TigrisReader.java
 * Description:   parses rules objects from strings.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

import com.tigrisgame.ai.SearchViz;

import java.io.*;

public class TigrisReader 
{
   Reader in=null;

   public TigrisReader( String str )
   {
      this.in = new StringReader( str );
   }

   public TigrisReader( Reader reader )
   {
      if ( reader.markSupported())
	 this.in = reader;
      else 
	 this.in = new BufferedReader( reader );
   }

   public MoveHistory readMoveHistory()
      throws IOException
   {
      char pk;
      if ( peekChar() != 'M' || !readLine().endsWith("START"))
	 throw new IOException("expected MoveHistory START" );

      MoveHistory history = new MoveHistory();
      history.gameStart( readGame());
      while( (pk=peekChar()) != 'M' ) {
	 Move move = readMove();
	 history.moveMade( move );
      }

      String line = readLine();
      //System.out.println("last line ("  + new Character( pk) +") :" + line);
      if ( !line.endsWith("END"))
	 throw new IOException("expecting MoveHistory END" );

      return history;
   }

   // A Game has the following tokens '/' delimited.
   // - String gameID.
   // - Player[] players. (delim by [] and commas)
   // - int gameState.
   // - int actionNumber.
   // - int actionDynasty.
   // - int moveDynasty.
   // - boolean tileBagWasExhausted
   // - TileBag tileBag (usually ordered)
   // - Location lastSpace (must have for ext conflicts & monument detection) 
   // (needed to reconstruct Conflict, int-vs-ext can be inferred by join tile)
   // - Sphere conflictSphere 
   // - Sphere attackScreenSupport (needed to reconstruct Conflict)
   // - Board board

   public TigrisGame readGame()
      throws IOException
   {
      StringBuffer strbuf = new StringBuffer();
      char cc = 0;

      int numPlayers = readCharAsInt();
      readChar(); // read delim.
      StandardGame game = new StandardGame( numPlayers );
      for ( int ii=0; ii<numPlayers; ii++) {
	 game.setPlayer( readPlayer() );
      }
      
      game.setState( readCharAsInt() );
      game.setActionNumber( readCharAsInt() );
      game.setActionDynasty( readCharAsInt() );
      game.setMoveDynasty( readCharAsInt() );
      Sphere confSphere = readCharAsSphere();
      int attScreenSup = -1;
      if ( Character.isDigit(cc = readChar()))
	 attScreenSup = Integer.parseInt( "" + new Character(cc) );
      readChar(); // trailing delim.

      // LAST SPACE.
      Location lastSpace = readLocation();
      game.setLastSpace( lastSpace );
      readChar(); // trailing delim.

      // TILE BAG.
      game.setTileBagWasExhausted( readChar() == '1' );
      game.tileBag = readTileBag();

      // Board.
      game.board = readBoard();

      // after evertying read.
      // setConflict() object.
      if ( confSphere != null ) {
	 Conflict conf;
	 Kingdom king = game.getBoard().getKingdom( lastSpace );
	 if ( king == null)
	    conf = Conflict.newExternalConflict( game, confSphere );
	 else
	    conf = Conflict.newInternalConflict( game, game.getBoard().getSpace( lastSpace ).getLeader(), king.getLeader( confSphere ));
	 if ( attScreenSup != -1 ) {
	    TileBag support = new TileBag();
	    support.putTiles( conf.getSupportSphere(), attScreenSup);
	    conf.setAttackScreenSupport( support );
	 }
	 game.setConflict( conf );
      }
      game.rules.setGame( game );

      return game;
   }

   public Board readBoard()
      throws IOException
   {
      Board2 board = null;
      LocationImpl loc = new LocationImpl( 0,0 );
      Sphere sphere = null, sphere2 = null;
      int dynasty = 0;
      char cc = 0;

      int rows = -1, cols = -1;
      int tmpNum = 0;
      while ( true ) {
	 cc = readChar();
	 if ( cc == '/' ) {
	    if ( rows == -1 ) {
	       rows = tmpNum;
	       tmpNum = 0;
	       continue;
	    }
	    else { // ( cols == -1 )
	       cols = tmpNum;
	       break;
	    }
	 }
	 tmpNum = tmpNum*10;
	 tmpNum += Integer.parseInt( "" + new Character(cc));
      }

      board = new Board2( rows,cols );

      // TOOD
      // parse ROWS and COLS
// 	 num = num*10;
// 	 num += Integer.parseInt( "" + new Character((char)readChar));
//     player.dynasty = Integer.parseInt( "" + new Character((char)readChar));
//     player.dynasty = Integer.parseInt( "" + new Character((char)readChar));

      loop:
      while ( true ) {
	 cc = readChar();
	 switch( cc) {
	 case '.':
	    board.emptyLocation( loc, false );
	    break; // already empty.
	 case '~':
	    board.emptyLocation( loc, true );
	    break;
	 case 'b':
	 case 'r':
	 case 'u':
	 case 'g':
	 case 'B':
	 case 'R':
	 case 'U':
	 case 'G':
	    sphere = Sphere.getSphereFromChar( cc );
	    board.putTile( loc, sphere );
	    break;
	 case '0':
	 case '1':
	 case '2':
	 case '3':
	    dynasty = Integer.parseInt( "" + new Character(cc));
	    sphere = readCharAsSphere();
	    // invalid format might give null sphere and thus throw runtime.
	    board.putLeader( loc, sphere, dynasty );
	    break;
	 case 'x':
	 case 'X':
	    board.putCatastrophe( loc );
	    break;
	 case '*':
	 case '+':
	    board.putTile( loc, Sphere.RED );
	    board.putTreasure( loc, cc == '*' );
	    break;
// 	 case 'N':
// 	 case 'n':
// 	    cc = readChar();
// 	    board.putTreasure( loc, cc == '*' );
// 	    // fall through on purpose.
	 case 'M':
	 case 'm':
	    cc = readChar();
	    {  // check treasure in the first loc.
	       if ( cc == '*' ) board.putTreasure( loc, true );
	       else if ( cc == '+' ) board.putTreasure( loc, false );
	    }
	    sphere = Sphere.getSphereFromChar( cc );
	    cc = readChar();
	    sphere2 = Sphere.getSphereFromChar( cc );
	    board.putMonument( loc, sphere, sphere2 );
	    loc.setCol( loc.getCol() + 1 ); // increment the location.
	    {  // check treasure in the second loc.
	       if ( cc == '*' ) board.putTreasure( loc, true );
	       else if ( cc == '+' ) board.putTreasure( loc, false );
	    }
	    board.putMonument( loc, sphere, sphere2 ); // place again.
	    break;
	 case 'J':
	 case 'j':
	    sphere = readCharAsSphere();
	    board.putJoinTile( loc, sphere );
	    break;
	 case '/':
	    loc.setCol( 0 );
	    loc.setRow( loc.getRow() + 1 );
	    if ( loc.getRow() == board.getNumRows() )
	       break loop;
	    else
	       continue;
	 default:
	    // unexpected EOF or invalid char.
	    System.out.println("got invalid char " + new Character( cc ));
	    return null;
	 }

	 loc.setCol( loc.getCol() + 1 ); // increment the location.

      }
      
      // check to see if we filled every row ??
      board.updateKingdoms();

      return board; 
   }

   public OrderedTileBag readTileBag()
      throws IOException
   {
      OrderedTileBag tiles = new OrderedTileBag();
      int readByte;
      Sphere ss;
      while( (readByte = in.read()) != '/' && readByte != -1 &&
	     readByte != '\n' && readByte != '\r' ) {
	 if ( readByte == '-' ) { 
	    // this is the mark for where the next draw is.
	    tiles.setNextDraw( tiles.getNumTiles());
	    continue;
	 }
	 ss = Sphere.getSphereFromChar( (char) readByte );
	 if ( ss == null )
	    throw new IOException("expected sphere letter: " + readByte + " " +
				  new Character( (char)readByte ));

	 tiles.putTiles( ss, 1 );
      }
      //      System.out.println("read tiles " + tiles );


      return tiles;
   }

   public Player readPlayer()
      throws IOException
   {
      Player player = new Player();
      Sphere sphere = null;
      char cc = 0;

      // read dynasty.
      try {
	 cc = readChar();
	 int dyn = Integer.parseInt( ""+new Character(cc));
	 player.setDynasty( dyn );
	 readChar(); // read '/' delim.
      } catch (NumberFormatException nfe) {
	 throw new IOException( "expected digit as dynasty for first char");
      }

      // read name
      StringBuffer name = new StringBuffer(); 
      while ( true ) {
	 cc = readChar();
	 if ( cc == '/' ) break;
	 name.append( new Character( cc ));
      }
      player.setName( name.toString());

      // read leaders.
      while ( true ) {
	 cc = readChar();
	 if ( cc == '/' ) break;
	 if ( cc == -1 ) throw new EOFException("unexpected eof");
	 sphere = Sphere.getSphereFromChar( cc );
	 if ( sphere == null ) {
	    throw new IOException("expected brug char for leader dynasty");
	 }
	 player.addLeader( sphere );
      }

      // read tiles 
      while ( true ) {
	 cc = readChar();
	 if ( cc == '/' ) break;
	 sphere = Sphere.getSphereFromChar( cc );
	 if ( sphere != null )
	    player.putTile( sphere );
	 else if ( cc == 'X' )
	    player.putCatastrophe();
      }

      // read vps (and treasures)
      long tmpNum=0;
      Sphere [] spheres = Sphere.getSpheres(); 
      for ( int ss=0; ss<spheres.length+2; ) {
	 cc = readChar();

	 if ( cc == '/' ) {
	    // unroll the number into the appropriate category.
	    if ( ss < spheres.length ) {
	       while ( tmpNum-- > 0 )  
		  player.addVictoryPoint( spheres[ss] );
	    }
	    else if ( ss == spheres.length ) {
	       while ( tmpNum-- > 0 )  
		  player.addTreasure();
	    }
	    else {
	       player.setClock( tmpNum );
	    }
	    ss ++;
	    tmpNum = 0;
	    continue; 
	 }
	 tmpNum = tmpNum*10;
	 tmpNum += Integer.parseInt( "" + new Character(cc));
      }

      //System.out.println("read player " + player );

      return player;
   }

   // this is a very barebones parser that reads the somewhat concise
   // Move.toString() output.  a move thorough human input parser is
   // found in the move command.
   public Move readMove()
      throws IOException
   {
      Move move = null;
      Sphere s1 = null;
      Sphere s2 = null;
      Location space = null;
      int dynasty = readCharAsInt();

      switch (readChar()) {
      case 'l':
      case 'L':
	 s1 = readCharAsSphere();
	 space = readLocation();
	 move = Move.newPositionLeader( dynasty, space, s1 );
	 break;
      case 't':
      case 'T':
	 s1 = readCharAsSphere();
	 space = readLocation();
	 move = Move.newPlaceTile( dynasty, space, s1 );
	 break;
      case 'c':
      case 'C':
	 space = readLocation();
	 move = Move.newPlayCatastrophe( dynasty, space );
	 break;
      case 's':
      case 'S':
	 move = Move.newSwapTiles( dynasty, readTileBag() );
	 break;
      case 'r':
      case 'R':
	 move = Move.newChooseTreasure( dynasty, readLocation());
	 break;
      case 'o':
      case 'O':
	 s1 = readCharAsSphere();
	 move = Move.newChooseSphere( dynasty, s1 );
	 break;
      case 'a':
      case 'A':
	 move = Move.newChooseSupporters( dynasty, readTileBag() );
	 break;
      case 'm':
      case 'M':
	 s1 = readCharAsSphere();
	 if ( s1 != null ) {
	    s2 = readCharAsSphere();
	    move = Move.newBuildMonument( dynasty, readLocation(), 
					  s1, s2 );
	 } else {
	    move = Move.newBuildMonument( dynasty, null, null, null );
	 }
	 break;
      default:
	 throw new IOException( "expected char [l,t,c,s] or [r,o,a,m]");
      }
      return move;
   }

   // reads a location of the form 'b3' or 'd11' and returns a LocationImpl,
   // otherwise read 2 chars and return null.
   public Location readLocation()
      throws IOException
   {
      char cc = readChar();  // first char (expecting letter)
      if ( Character.isLetter( cc )) {
	 int row = (int) (cc - 'a');
	 int col = readCharAsInt(); // 2nd char (expecting digit)
	 in.mark(16); // possible 3rd char.
	 int readByte = in.read();
	 if ( readByte != -1 && 
	      Character.isDigit((cc=(char)readByte))) { // check for 10's case.
	    col = col*10 + Integer.parseInt( ""+new Character(cc));
	 } else {
	    in.reset(); // rollback, didn't want the third char.
	 }
	 return new LocationImpl(row,col-1);
      } else {
	 readChar();  // read 2nd char.
	 return null;
      }
   }

   public String readLine()
      throws IOException
   {
      StringBuffer strbuf = new StringBuffer();
      int readByte = 0;
      char readChar = 0;

      while ( true ) {
	 readByte = in.read();
	 readChar = (char) readByte;

	 if ( Character.isWhitespace( readChar ) 
	      && strbuf.length() == 0 ) continue;
	 if ( readByte == -1 || readChar == '\n' ) break;

	 // otherwise...
	 strbuf.append( readChar );
      }
      return strbuf.toString();
   }

   public char peekChar()
      throws IOException
   {
      char cc;
      in.mark(16);
      cc = readChar();
      in.reset();
      return cc;
   }

   // custom read char method, throws IO exception on EOF 
   // and chomps through whitespace.
   public char readChar()
      throws IOException
   {
      int readByte = 0;
      char readChar = 0;

      while ( true ) {
	 readByte = in.read();
	 readChar = (char) readByte;

	 if ( readByte == -1 ) throw new EOFException("unexpected eof");
	 if ( Character.isWhitespace( readChar )) continue;

	 break;
      }
//       System.out.print("(" + new Character( readChar ) + ")");
//       System.out.flush();
      return readChar;
   }

   public Sphere readCharAsSphere()
      throws IOException
   {
      return Sphere.getSphereFromChar( readChar() );
   }

   public int readCharAsInt()
      throws IOException
   {
      Character cc = new Character(readChar());
      
      try {
	 return Integer.parseInt( "" + cc );
      } catch (NumberFormatException nfe) {
	 throw new IOException( "unable to parse char " + cc + " as int");
      }
   }

   public void close()
   {
      try {
	 in.close();
      } catch (IOException ioe) {}
   }

   public static void main( String [] args )
   {
      String filename = "abc.txt";
      if ( args.length > 0 )
	 filename = args[0];

      try {
	 BufferedReader in = new BufferedReader
	    ( new FileReader( new File( filename)));
	 TigrisReader tr = new TigrisReader( in );

	 MoveHistory hist = tr.readMoveHistory();
	 SearchViz viz = new SearchViz( hist.getGame(0));
	 viz.initSearch(hist.getGame());
	 viz.wrapWithFrame();

	    //System.out.println( hist );

	 synchronized( viz ) {
	    viz.wait();
	 }

      } catch (Exception ex) {
	 System.out.println("" + ex );
	 //ex.printStackTrace();
      }

   }
}








