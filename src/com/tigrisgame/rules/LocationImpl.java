/* File: LocationImpl.java
 * Description:  Wrapper for x,y location (implementation)
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.rules;

import java.io.Serializable;

public class LocationImpl implements Location, Serializable
{
   private int m_row = -1;
   private int m_col = -1;

    // empty constructor needed for serialization.
    public LocationImpl() {}
   
   public LocationImpl( int row, int col )
   {
      m_row = row;
      m_col = col;
   }

   public LocationImpl( Location loc )
   {
      m_row = loc.getRow();
      m_col = loc.getCol();
   }

   public int getRow()
   {
      return m_row;
   }

   public int getCol()
   {
      return m_col;
   }

   public void setRow( int row )
   {
      m_row = row;
   }

   public void setCol( int col )
   {
      m_col = col;
   }
   
   public boolean isLocationEqual( Location sp )
   {
      return ( sp != null &&
	       m_row == sp.getRow() &&
	       m_col == sp.getCol() );
   }

   public String toString()
   {
      return formatLocation( this );
   }

   public static String formatLocation( Location sp )
   {
      return formatLocation( sp, "" );
   }

   public static String formatLocation( Location sp, String delim )
   {
       if ( sp == null ) return "null";

      String colStr = "" + (sp.getCol() +1);
      Character rowChar =  new Character( (char) ('a' + sp.getRow()));

      // old chess style grid coords.
//       Character colChar = new Character( (char) ('a' + (sp.getCol())));
//       String rowStr =  "" + (board.getNumRows() -  sp.getRow());
      return new String ( rowChar + delim + colStr );
   }
}



