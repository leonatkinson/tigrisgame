/* File: GameEvent.java
 * Description:  Event object for changes to Game state.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

public class GameEvent
{
    /***************
     * Event Types *
     ***************/

    // primary player moves.
    public static final int LEADER_PLACED      = 101;
    public static final int LEADER_MOVED       = 102;
    public static final int LEADER_WITHDRAWN   = 103;
    public static final int TILE_PLACED        = 104;
    public static final int TILES_SWAPPED      = 105;
    public static final int CATASTROPHE_PLACED = 106;

    // secondary player moves.
    public static final int MONUMENT_BUILT     = 201;
    public static final int NO_MONUMENT_BUILT  = 202;
    public static final int TREASURE_TAKEN     = 203;
    public static final int ATTACK_SUPPORT_SET = 204;
    public static final int DEFEND_SUPPORT_SET = 205;
    public static final int CONFLICT_CHOSEN    = 206;

    // move consequences.
    public static final int LEADER_ORPHANED    = 301;
    public static final int TILES_DRAWN        = 302;
    public static final int TILE_VP_AWARDED    = 303;

    // conflict/join related.
    public static final int KINGDOM_JOIN_CAUSED   = 401;
    public static final int KINGDOM_JOIN_RESOLVED = 402;
    public static final int EXTERNAL_CONFLICT     = 403;
    public static final int INTERNAL_CONFLICT     = 404;
    public static final int CONFLICT_VP_AWARDED   = 406;
    public static final int CONFLICT_WON          = 407;
    public static final int CONFLICT_LOST         = 408;

    // end of action/turn.
    public static final int MONUMENT_VP_AWARDED   = 501;
    public static final int ACTION_FINISHED       = 502;
    public static final int TURN_FINISHED         = 503;
    public static final int GAME_OVER             = 504;

    // event variables.
    TigrisGame source;
    int type;
    Object obj1;
    Object obj2;
    Object obj3;

    public GameEvent( TigrisGame source, int type )
    {
	this( source, type, null, null, null );
    }

    public GameEvent( TigrisGame source, int type, Object obj1 )
    {
	this( source, type, obj1, null, null );
    }

    public GameEvent( TigrisGame source, int type, Object obj1, Object obj2 )
    {
	this( source, type, obj1, obj2, null );
    }

    public GameEvent( TigrisGame source, int type, Object obj1, 
		      Object obj2, Object obj3 )
    {
	this.source = source;
	this.type = type;
	this.obj1 = obj1;
	this.obj2 = obj2;
	this.obj3 = obj3;

	validateEventType();
    }

    public void validateEventType()
	throws IllegalArgumentException
    {
	switch ( type ) {
	case LEADER_PLACED:
            if ( ! (obj1 instanceof Leader) ||
		 ! (obj2 instanceof Location ) ||
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "Leader, Location  as arguments" );
		    break;
	case LEADER_MOVED:
            if ( ! (obj1 instanceof Leader) ||
		 ! (obj2 instanceof Location ) ||
		 ! (obj3 instanceof Location ))
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "Leader, Location, Location as arguments" );
		    break;
	case LEADER_WITHDRAWN:
            if ( ! (obj1 instanceof Leader) ||
		 obj2 != null  ||
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "Leader " + "as arguments" );
		    break;
	case TILE_PLACED:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Sphere ) ||
		 ! (obj3 instanceof Location ) )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, sphere, location as arguments" );
		    break;
	case TILES_SWAPPED:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof TileHolder ) ||
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, tileholder  as arguments" );
		    break;
	case CATASTROPHE_PLACED:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Location ) ||
		 ! (obj3 == null || obj3 instanceof Tile ))
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, location, <tile>  as arguments" );
		    break;	    
	case MONUMENT_BUILT:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Monument ) ||
		 ! (obj3 instanceof Location ))
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, monument, location  as arguments" );
		    break;	    
	case NO_MONUMENT_BUILT:
	    if ( ! (obj1 instanceof Integer) ||
		 obj2 != null || 
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int as arguments" );
		    break;	    
	case TREASURE_TAKEN:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Location ) ||
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, location  " + "as arguments" );
		    break;	    
	case ATTACK_SUPPORT_SET:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Integer ) ||
		 ! (obj3 instanceof Integer ))
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, int, int " + "as arguments" );
		    break;	    
	case DEFEND_SUPPORT_SET:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Integer ) ||
		 ! (obj3 instanceof Integer ))
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, int, int " + "as arguments" );
		    break;	    
	case CONFLICT_CHOSEN:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Integer ) ||
		 ! (obj3 instanceof Integer ))
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, int, int " + "as arguments" );
	    break;	
	case LEADER_ORPHANED:
	    if ( ! (obj1 instanceof Leader) ||
		 ! (obj2 instanceof Location ) ||
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "leader, location, as arguments" );
	    break;
	case TILES_DRAWN:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof TileHolder ) ||
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, tileholder " + "as arguments" );
		    break;	
	case TILE_VP_AWARDED:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Sphere ) ||
		 ! (obj2 instanceof Sphere ))
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, sphere, sphere, as arguments" );
		    break;	
	case KINGDOM_JOIN_CAUSED:
	    if ( ! (obj1 instanceof Location) ||
		 ! (obj2 instanceof Kingdom ) ||
		 ! (obj3 instanceof Kingdom ))
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "location, king, king " + "as arguments" );
		    break;	
	case KINGDOM_JOIN_RESOLVED:
	    if ( ! (obj1 instanceof Location) ||
		 obj2 != null ||
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "location " + "as arguments" );
		    break;	
	case INTERNAL_CONFLICT:
	case EXTERNAL_CONFLICT:
	    if ( ! (obj1 instanceof Leader) ||
		 ! (obj2 instanceof Leader ) ||
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "leader, leader " + "as arguments" );
	    break;	
// 	case TILE_REMOVED:
// 	    if ( ! (obj1 instanceof Location) ||
// 		 obj2 != null ||
// 		 obj3 != null )
// 		throw new IllegalArgumentException
// 		    ( "type " + type + " must have " +
// 		      "location " + "as arguments" );
// 	    break;	
	case CONFLICT_VP_AWARDED:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Sphere) ||
		 ! (obj3 instanceof Integer))
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, sphere, int" + "as arguments" );
	    break;	
	case CONFLICT_WON:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Sphere) || 
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, sphere as arguments" );
	    break;	
	case CONFLICT_LOST:
	    if ( ! (obj1 instanceof Integer)  ||
		 ! (obj2 instanceof Sphere) ||
		 obj3 != null)
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, sphere as arguments" );
	    break;	

	case MONUMENT_VP_AWARDED:
	    if ( ! (obj1 instanceof Integer) ||
		 ! (obj2 instanceof Sphere) ||
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int, sphere" + "as arguments" );
	    break;	
	case ACTION_FINISHED:
	    if ( ! (obj1 instanceof Integer) ||
		 obj2 != null || 
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int " + " as arguments" );
	    break;
	case TURN_FINISHED:
	    if ( ! (obj1 instanceof Integer) ||
		 obj2 != null || 
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "int " + " as arguments" );
	    break;
	case GAME_OVER:
	    if ( ! ( obj1 instanceof Integer) ||
		 obj2 != null || 
		 obj3 != null )
		throw new IllegalArgumentException
		    ( "type " + type + " must have " +
		      "no " + "arguments" );
	    break;
	default:
	    throw new IllegalArgumentException("Unknown type " + type );
	}
    }

   public static void processGameEvent( GameListener listener,
					GameEvent event )
   {
      switch ( event.type ) {
	 
      case LEADER_PLACED:
	 listener.leaderPlaced( (Leader) event.obj1,
				(Location) event.obj2 );
	 break;
      case LEADER_MOVED:
	 listener.leaderMoved( (Leader) event.obj1,
			       (Location) event.obj2,
			       (Location) event.obj3);
	 break;
      case LEADER_WITHDRAWN:
	 listener.leaderWithdrawn( (Leader) event.obj1 );
	 break;
      case TILE_PLACED:
	 listener.tilePlaced( ((Integer)event.obj1).intValue(),
			      (Sphere) event.obj2,
			      (Location) event.obj3 );
	 break;
      case TILES_SWAPPED:
	 listener.tilesSwapped( ((Integer)event.obj1).intValue(),
				(TileHolder) event.obj2 );
	 break;
      case CATASTROPHE_PLACED:
	 listener.catastrophePlaced( ((Integer)event.obj1).intValue(),
				     (Location) event.obj2,
				     (Tile) event.obj3);
	 break;	    
      case MONUMENT_BUILT:
	 listener.monumentBuilt( ((Integer)event.obj1).intValue(),
				 (Monument) event.obj2,
				 (Location) event.obj3 );
	 break;	    
      case NO_MONUMENT_BUILT:
	 listener.noMonumentBuilt( ((Integer)event.obj1).intValue());
	 break;	    
      case TREASURE_TAKEN:
	 listener.treasureTaken( ((Integer)event.obj1).intValue(),
				 (Location) event.obj2 );
	 break;
      case ATTACK_SUPPORT_SET:
	 listener.attackSupportSet( ((Integer)event.obj1).intValue(),
				    ((Integer)event.obj2).intValue(),
				    ((Integer)event.obj3).intValue() );
	 break;	    
      case DEFEND_SUPPORT_SET:
	 listener.defendSupportSet( ((Integer)event.obj1).intValue(),
				    ((Integer)event.obj2).intValue(),
				    ((Integer)event.obj3).intValue() );
	 break;	    
      case CONFLICT_CHOSEN:
	 listener.conflictChosen( ((Integer)event.obj1).intValue(),
				     (Sphere) event.obj2 );
	 break;	
      case LEADER_ORPHANED:
	 listener.leaderOrphaned( (Leader) event.obj1, 
				  (Location) event.obj2 );
	 break;	
      case TILES_DRAWN:
	 listener.tilesDrawn( ((Integer)event.obj1).intValue(),
			      (TileHolder) event.obj2 );
	 break;	
      case TILE_VP_AWARDED:
	    listener.tileVPAwarded( ((Integer)event.obj1).intValue(),
				    (Sphere) event.obj2,
				    (Sphere) event.obj3);
	    break;	
      case KINGDOM_JOIN_CAUSED:
	 listener.kingdomJoinCaused( (Location) event.obj1,
				     (Kingdom) event.obj2,
				     (Kingdom) event.obj3 );
	 break;	
      case KINGDOM_JOIN_RESOLVED:
	 listener.kingdomJoinResolved( (Location) event.obj1 );
	 break;	
      case INTERNAL_CONFLICT:
	 listener.internalConflict( (Leader) event.obj1, 
				    (Leader) event.obj2 );
	 break;
      case EXTERNAL_CONFLICT:
	 listener.externalConflict( (Leader) event.obj1, 
				    (Leader) event.obj2 );
	 break;
//       case TILE_REMOVED:
// 	 listener.tileRemoved( (Location) event.obj1 );
// 	 break;	
      case CONFLICT_VP_AWARDED:
	 listener.conflictVPAwarded( ((Integer)event.obj1).intValue(),
				     (Sphere) event.obj2,
				     ((Integer)event.obj3).intValue() );
	 break;	
      case CONFLICT_WON:
	 listener.conflictWon( ((Integer)event.obj1).intValue(),
			       (Sphere) event.obj2 );
	 break;	
      case CONFLICT_LOST:
	 listener.conflictLost( ((Integer)event.obj1).intValue(),
				(Sphere) event.obj2);
	 break;	
      case MONUMENT_VP_AWARDED:
	 listener.monumentVPAwarded( ((Integer)event.obj1).intValue(),
				     (Sphere) event.obj2 );
	 break;	
      case ACTION_FINISHED:
	 listener.actionFinished( ((Integer)event.obj1).intValue() );
	 break;
      case TURN_FINISHED:
	 listener.turnFinished( ((Integer)event.obj1).intValue() );
	 break;
      case GAME_OVER:
	 listener.gameOver( ((Integer) event.obj1).intValue() );
	 break;
      }
   }
    
    public String toString()
    {
	return ("GameEvent " + type + " " + obj1 + " " + obj2 + " " + obj3);
    }
}
