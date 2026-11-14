/* File: FWTWRParser.java
 * Description:  Parses FWTWR weblogs.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.util;

import com.tigrisgame.rules.*;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.awt.Point;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class FWTWRParser
{
    //LinkedList allMoves = new LinkedList();
    Vector allMoves = new Vector();
    Vector turnMoves;

    static String [] leaderNames = { "King", "Priest", "Farmer", "Trader" };
    static String [] tileNames = { "Settlement", "Temple", "Farm", "Market" };
    static String [] colorNames = { "Black", "Red", "Blue", "Green" };
    static Sphere [] spheres = Sphere.getSpheres();
    String [] playerNames = new String[4];
    Board board = (new StandardGame( new GameOptions())).getBoard();
    int verbosity = 2;

    public static void main( String [] args )
	throws ParserConfigurationException, IOException, SAXException 
    {
	new FWTWRParser().parse( args[0] );
    }

    // get the first child node with this name.
    public Node getNode( Node node, String name )
    {
	return getNode( node, name, 1 );
    }

    public Node getNode( Node node, String name, int matchNum )
    {
	int matchCount = 0;
	NodeList nodeChildren = node.getChildNodes();
	for ( int ii=0; ii<nodeChildren.getLength(); ii++ ) {
	    Node testNode = nodeChildren.item( ii );
	    if ( name.equals( testNode.getNodeName() )) {
		matchCount ++;
		if ( matchCount == matchNum )
		    return testNode;
	    }

	}
	return null;
    }

    public int getDynasty( String name )
    {
	for ( int ii=0; ii<playerNames.length; ii++ ) {
	    if ( playerNames[ii].startsWith( name ) )
		return ii;
	}
	return -1;
    }

    public void parse( String filename )
	throws ParserConfigurationException, IOException, SAXException 
    {
	MoveHistory history = new MoveHistory();

	try {
	    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	    DocumentBuilder db = dbf.newDocumentBuilder();
	    FileInputStream in = new FileInputStream( filename );
	    Document doc = db.parse( in, null);
	    System.out.println( " === read " + filename + " okay! === ");

	    Element root = doc.getDocumentElement();
	    Node body = getNode( root, "body" );

  	    NodeList bodyChildren = body.getChildNodes();
	    Node moveTable = null;
	    Node dynTable = null;
  	    for ( int ii=0; ii<bodyChildren.getLength(); ii++ ) {
  		Node node = bodyChildren.item( ii );
		if ( ii == 9 )
		    moveTable = node;
		if ( ii == 5 )
		    dynTable = node;

  		//System.out.println("child " + ii + " is " + node.getNodeName());
  	    }	    

	    // somewhat nested.
	    dynTable = getNode( dynTable, "center" );
	    dynTable = getNode( dynTable, "table" );
	    dynTable = getNode( dynTable, "tr" );
	    dynTable = getNode( dynTable, "td", 2 );
	    dynTable = getNode( dynTable, "table" );
	    NodeList dynTableChildren = dynTable.getChildNodes();
  	    for ( int ii=0; ii<dynTableChildren.getLength(); ii++ ) {
  		Node node = dynTableChildren.item( ii );

		if ( "tr".equals( node.getNodeName() ) )
		    processDynastyRow( node );
  	    }	    

  	    NodeList moveTableChildren = moveTable.getChildNodes();
	    Node turn = null;
  	    for ( int ii=0; ii<moveTableChildren.getLength(); ii++ ) {
  		Node node = moveTableChildren.item( ii );

		if ( "tr".equals( node.getNodeName() ) )
		    processTurnRow( node );
  	    }	    

	    int numPlayers = 0;
	    for ( int ii=0; ii<playerNames.length; ii++ )
		if ( playerNames[ii] != null ) numPlayers ++;


	    //ListIterator listit = allMoves.listIterator( 0 );
	    Enumeration listit = allMoves.elements();
	    TigrisGame game = new StandardGame( numPlayers );
	    Player tmpPlayer = null;
	    for ( int ii=0; ii<numPlayers; ii++) {
		tmpPlayer = game.getPlayer( ii );
		tmpPlayer.setName( playerNames[ii] );
	    }

	    history.gameStart( game );
	    history.hideTiles();		

	    game = history.getGame();
	    while ( listit.hasMoreElements() ) {
		Move move = (Move) listit.nextElement();
		// skip redundant choose_conflict_sphere moves.
		if ( move.isChooseSphere() && 
		     history.getGame().getState() !=  
		     TigrisGame.WAIT_CONFLICT_ORDER ) {
		    if ( verbosity > 1 ) 
			System.out.println("skipping move " + move );
		}
		else {
		    if ( verbosity > 1 ) 
			System.out.println("making move " + move );
		    history.moveMade( move );
		}
	    }

	    if ( verbosity > 0 ) 
		System.out.println("" + history );

//   	    printTree( root, 0 );


	} catch (SAXParseException spe) {
	    System.out.println("error was at line " + spe.getLineNumber() 
			       + " col " + spe.getColumnNumber() );
	    spe.printStackTrace();
//  	} catch (IllegalMoveException ime ) {
//  	    ime.printStackTrace();
// 	} catch (RuntimeException re ) {
// 	    System.out.println("aborting reading/making of moves"); 
 	} 

    
	try {

	    String outFile = filename;
	    if ( filename.indexOf( "." ) != -1 )
		outFile = outFile.substring( 0, filename.indexOf( "." ) );
	    outFile += ".tgm";

	    ObjectOutputStream out =
		new ObjectOutputStream( new FileOutputStream( outFile ));
	    out.writeObject( history );
	    out.close();

 	} catch( IOException ioe ) {
 	    ioe.printStackTrace();
 	} 

    }

    void processDynastyRow( Node node )
    {
// 	System.out.println("======================");
// 	printTree( node, 0 );

	Node imageCol = getNode( node, "td" );
	Node nameCol = getNode( node, "td", 2 );

	String name = null;
	if ( getNode( imageCol, "img" ) != null && 
	     nameCol != null) {
	    name = getNode( nameCol, "#text" ).getNodeValue();

	    for ( int ii=0; ii<playerNames.length; ii++) {
		if ( playerNames[ii] != null )
		    continue;

		playerNames[ii] = name;
		if ( verbosity > 1 ) 
		    System.out.println( "DYNASTY " + ii + " IS " + name );
		break;
	    }
	}
    }

    void processTurnRow( Node turn )
    {
	NodeList turnChildren = turn.getChildNodes();
	int tdNum = 0;

	String date = null;
	String player = null;

	for ( int ii=0; ii<turnChildren.getLength(); ii++ ) {
	    Node node = turnChildren.item( ii );

	    if ( "td".equals( node.getNodeName() )) 
		tdNum ++;
	    else 
		continue;

	    if ( node.hasChildNodes() ) {
		switch ( tdNum ) {
		case 1:
		    date = node.getFirstChild().getNodeValue();
		    break;
		case 2:
		    player = node.getFirstChild().getNodeValue();
		    break;
		case 3:
		    processMoveCol( node, player );
		    break;
		default:
		}
	    }

	}	    

	if ( date == null && player == null )
	    return;

	//System.out.println("date = " + date + " player = " + player );
    }

    void processMoveCol( Node node, String turnPlayer )
    {
	NodeList nodeChildren = node.getChildNodes();
	StringBuffer lineBuf = new StringBuffer();
	Vector lines = new Vector();
	turnMoves = new Vector();
	
	for ( int ii=0; ii<nodeChildren.getLength(); ii++ ) {
	    Node childNode = nodeChildren.item( ii );

	    if ( "br".equals( childNode.getNodeName() )) {
		lines.addElement( lineBuf.toString() );
		lineBuf = new StringBuffer();
	    }

	    else if ( "#text".equals( childNode.getNodeName() )) {
		lineBuf.append( childNode.getNodeValue());
	    }
	}
	lines.addElement( lineBuf.toString() );

	for ( int ii=0; ii<lines.size(); ii++) {
	    String thisLine, prevLine = null; 
	    thisLine = (String) lines.elementAt( ii );
	    if ( ii > 0 )
		prevLine = (String) lines.elementAt( ii - 1 );

	    Move move = processMoveLine( thisLine, prevLine, turnPlayer );
	}

	// insert into the overall list.
	for ( int ii=turnMoves.size()-1; ii >= 0; ii-- ) {
	    allMoves.insertElementAt( turnMoves.elementAt( ii ), 0 );
	}
	
    }

    Move processMoveLine( String line, String prevLine, String turnPlayer )
    {
	Move move = null;
	line = line.trim();


	if ( verbosity > 1 )
	    System.out.println( line );

	/**************************
	 * check for IGNORE LINES *
	 **************************/
	if ( line.indexOf("wins.") != -1 ) {
	    // telling us who won conflict, ignore
	} else if ( line.indexOf("Awaiting orders") != -1 ) {
	    // telling us who's turn it is, ignore.
	} else if ( line.indexOf("Monuments:") != -1 ) {
	    // telling us monument vp, ignore.
	} else if ( line.indexOf("No further") != -1 ) {
	    // telling us external conflict is over.
	} else if ( line.indexOf( "action to come" ) != -1 ){
	    // telling us next action to come.
	}

	/*********************
	 * check for ACTIONS *
	 *********************/

	else if ( line.indexOf(" to " ) != -1 ) {
	    String what = null;

	    int dashIndex = line.lastIndexOf( "-" );
	    String beforeDash = line.substring( 0, dashIndex );

	    Point loc = parseLocation
		( line.substring( beforeDash.lastIndexOf(" "), 
				  dashIndex + 2 ));

	    for ( int ii=0; ii<leaderNames.length; ii++ ) {
		if ( line.startsWith( leaderNames[ii] + " " )) {
		    what = spheres[ii] + " leader"; 
 		    move = Move.newPositionLeader( getDynasty( turnPlayer ),
						   board.getSpace( loc.x, 
								   loc.y ),
						   spheres[ii] );
		}
	    }
		     
	    for ( int ii=0; ii<tileNames.length; ii++ ) {
		if ( line.startsWith( tileNames[ii] + " " ) ) {
		    what = spheres[ii] + " tile"; 
 		    move = Move.newPlaceTile( getDynasty( turnPlayer ),
					      board.getSpace( loc.x, 
							      loc.y ),
					      spheres[ii] );
		}
	    }

	    if ( line.startsWith("Catastrophe" )) {
		what = "catastrophe";

		move = Move.newPlayCatastrophe( getDynasty( turnPlayer ),
						board.getSpace( loc.x, 
								loc.y ));
	    }

	    if ( verbosity > 1 ) 
		System.out.println("turnMoves:" + move ); 
	    turnMoves.addElement( move );

	} else if ( line.startsWith( "Swaps" )) {
	    int numTiles = -1;
	    if ( line.indexOf( " none " ) != -1 )
		numTiles = 0;
	    else if ( line.indexOf( " one " ) != -1 )
		numTiles = 1;
	    else if ( line.indexOf( " two " ) != -1 )
		numTiles = 2;
	    else if ( line.indexOf( " three " ) != -1 )
		numTiles = 3;
	    else if ( line.indexOf( " four " ) != -1 )
		numTiles = 4;
	    else if ( line.indexOf( " five " ) != -1 )
		numTiles = 5;
	    else if ( line.indexOf( " six " ) != -1 )
		numTiles = 6;

	    TileBag tiles = new TileBag();
	    tiles.putTiles( Sphere.HIDDEN, numTiles );
	    move = Move.newSwapTiles( getDynasty( turnPlayer ), tiles );
	    if ( verbosity > 1 ) 
		System.out.println("turnMoves:" + move ); 
	    turnMoves.addElement( move );
	} 
	/***************************************
	 * check for TREASURE/MONUMENT/SUPPORT *
	 ***************************************/

	else if ( line.indexOf("Claims") != -1 ||
		  line.indexOf("claims") != -1 ) {
	    int dashIndex = line.indexOf( "-" );
	    String beforeDash = line.substring( 0, dashIndex );
	    Point loc = parseLocation( line.substring( beforeDash.
						       lastIndexOf(" "),
						       dashIndex + 2 ));

	    int dynasty = getDynasty( line.substring( 0, line.indexOf(" ")));
	    if ( dynasty == -1 )
		dynasty = getDynasty( turnPlayer );

	    // TODO ... what if gift treasure to non-turnPlayer ?? 	    
	    move = Move.newChooseTreasure( dynasty,
					   board.getSpace( loc.x, 
							   loc.y ));
	
	    if ( verbosity > 1 ) 
		System.out.println("turnMoves:" + move ); 
	    turnMoves.addElement( move );
	}
	else if ( line.indexOf("Build") != -1 ) {
	    int dashIndex = line.indexOf( "-" );
	    String beforeDash = line.substring( 0, dashIndex );
	    Point loc = parseLocation( line.substring( beforeDash.
						       lastIndexOf(" "),
						       dashIndex + 2 ));
	    
	    int slashIndex = line.indexOf( "/" );
	    Sphere sphere1 = parseSphere( line.substring( slashIndex, 
							  line.length() ));
	    Sphere sphere2 = parseSphere( line.substring( 0, slashIndex )); 

	    move = Move.newBuildMonument( getDynasty( turnPlayer ),
					  board.getSpace( loc.x, loc.y ),
					  sphere1, sphere2 );
	    if ( verbosity > 1 ) 
		System.out.println("turnMoves:" + move ); 
	    turnMoves.addElement( move );
	}
	else if ( line.indexOf("vs.") != -1 ) {
	    int tokenIndex = 0;
	    Sphere supportSphere = null;

	    // first see if if support order was chosen for external conflict.
	    // either it was a multi-conflict, 
	    // and we grab from first two words of this line.
	    for ( int ii=0; ii<leaderNames.length; ii++ ) {
		if ( line.startsWith( leaderNames[ii] )) {
		    supportSphere = spheres[ii];
		    move = Move.newChooseSphere( getDynasty( turnPlayer ),
						 supportSphere );
		    if ( verbosity > 1 ) 
			System.out.println("turnMoves:" + move ); 
		    turnMoves.addElement( move );
		    tokenIndex -= 2;
		}
	    }
	    
	    // or it was a single leader conflicting, 
	    // in this case, no support move is necessary.
	    // is external if external was mentioned in last line.
	    if( supportSphere == null && 
		prevLine.indexOf("external") != -1 ) {
		for ( int ii=0; ii<leaderNames.length; ii++ ) {
		    if ( prevLine.indexOf( leaderNames[ii] ) != -1) {
			supportSphere = spheres[ii];
			if ( verbosity > 1 ) 
			    System.out.println("conflict of only type:" +
					       supportSphere ); 
		    }
		}
	    }
	    
	    if ( supportSphere == null ){
		// otherwise conflict is internal.
		supportSphere = Sphere.RED;
	    }

	    if ( verbosity > 1 )
		System.out.println( "supportSphere is " + supportSphere );

	    StringTokenizer tokens = new StringTokenizer( line );
	    String conflictPlayer = null;
	    int screenSupport = -1;
	    TileBag supporters = null;
	    while ( tokens.hasMoreTokens() ) {
		// typical string will be "John (1 + 2 = 3) vs. Phillip (0 + 0 = 0)."
		String token = tokens.nextToken();  // won't work for john m.
		switch ( tokenIndex ) {
		case 0:
		    conflictPlayer = token;
		    break;
		case 1: // (1
		case 2: // +
		    break;
		case 3:
		    screenSupport = Integer.parseInt( token.trim() );
		    supporters = new TileBag();
		    supporters.putTiles( supportSphere, screenSupport );
		    move = Move.newChooseSupporters
			( getDynasty( conflictPlayer ), supporters );
		    if ( verbosity > 1 ) 
			System.out.println("turnMoves:" + move );
		    turnMoves.addElement( move );
		    break;
		case 4: // =
		case 5: // 3)
		case 6: // vs.
		    break;
		case 7:
		    conflictPlayer = token;
		case 8: // (0
		case 9: // +
		    break;
		case 10:
		    screenSupport = Integer.parseInt( token.trim() );
		    supporters = new TileBag();
		    supporters.putTiles( supportSphere, screenSupport );
		    move = Move.newChooseSupporters
			( getDynasty( conflictPlayer ), supporters );
		    if ( verbosity > 1 ) 
			System.out.println("turnMoves:" + move );
		    turnMoves.addElement( move );
		case 11: // =
		case 12: // 0).
		default:
		    break;
		}

		tokenIndex ++;
	    }

	}

	else {

	    System.out.println("*** HUH ***  " + line );
	}

	return null;
	
    }

    public static Sphere parseSphere( String sphereStr )
    {
	System.out.println("parseSphere " + sphereStr );

	for ( int ii=0; ii<colorNames.length; ii++ ) {
	    if ( sphereStr.indexOf( colorNames[ii] ) != -1)
		 return spheres[ii];
	}
	return null;
    }

    public static Point parseLocation( String locStr )
    {
	//System.out.println("parseLocation " + locStr );

	int dashIndex = locStr.indexOf( "-" );
	String beforeDashStr = locStr.substring( 0, dashIndex );
	String afterDashStr = locStr.substring( dashIndex, locStr.length() );
	    
	String rowStr = locStr.substring( beforeDashStr.lastIndexOf(" "), 
					  beforeDashStr.length() );
	int col = Integer.parseInt( rowStr.trim() ) - 1;
	char rowChar = afterDashStr.charAt( 1 );
	int row = rowChar - 'A';

	return new Point( row, col );
    }

    public static void printTree( Node node, int depth )

    {
	StringBuffer buf = new StringBuffer();

	buf.append("_");
	for ( int ii=0; ii<depth; ii++ )
	    buf.append(".");

	buf.append("_ ");
	buf.append(node.getNodeName());
	buf.append(" = ");
	buf.append(node.getNodeValue());
	System.out.println(buf.toString());

	NodeList children = node.getChildNodes();
	for ( int ii=0; ii<children.getLength(); ii++ ) {
	    Node childNode = children.item( ii );
	    printTree( childNode, depth + 1 );
	}
    }
}
