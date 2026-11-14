/* File: TextUtils.java
 * Description:  Text formating util class.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.util;


public class TextUtils 
{
   public static final int LEFT = 0;
   public static final int CENTER = 1;
   public static final int RIGHT = 2;

   public static String fitCenter( String str, int size )
   {
      return fit( str, size, CENTER );
   }
      
   public static String fitRight( String str, int size )
   {
      return fit( str, size, RIGHT );
   }

   public static String fitLeft( String str, int size )
   {
      return fit( str, size, LEFT );
   }

   // Copied significant portions from textui.ColorTextGame
   protected static String fit( String str, int size, int pos )
   {
      if ( str == null )
	 str = "";
      if ( str.length() > size )
	 return str.substring(0,size);
      
      StringBuffer strbuf = new StringBuffer(size);
      for ( int ii=0; ii<size; ii++) strbuf.insert(ii, " ");
      
      int leftPos = 0;
      if ( pos == CENTER ) 
	 leftPos = Math.max( (size - str.length())/2, 0 );
      else if ( pos == RIGHT )
	 leftPos = Math.max( size - str.length(), 0 );
      
      strbuf.replace( leftPos, Math.min( leftPos + str.length(), size), str );
      return strbuf.toString();
   }

   public static String formatTime(long ltime, int maxFields )
   {
      int days, hrs, mins, secs;

      int time = (int)(ltime / 1000);  // convert to seconds (and cast to int).
      int fields = 0;

      secs = time % 60; // extract seconds.
      time = time / 60; // convert to minutes.

      mins = time % 60; // extract mins.
      time = time / 60; // convert to hours.

      hrs = time % 24; // extract hours.
      days = time / 24; // convert to days.

      StringBuffer strbuf = new StringBuffer();
      String plurar = "s, ";
      String singular = ", ";
      if ( days > 0 && fields < maxFields ) {
	 strbuf.append( days + " day");
	 fields ++;
	 if ( days == 1 )
	    strbuf.append( singular );
	 else
	    strbuf.append( plurar );
      }
      if ( (hrs > 0 || strbuf.length() > 0)&& fields < maxFields ) {
	 strbuf.append( hrs + " hr");
	 fields ++;
	 if ( hrs == 1 )
	    strbuf.append( singular );
	 else
	    strbuf.append( plurar );
      }
      if ( (mins > 0 || strbuf.length() > 0) && fields < maxFields ) {
	 strbuf.append( mins + " min");
	 fields ++;
	 if ( mins == 1 )
	    strbuf.append( singular );
	 else
	    strbuf.append( plurar );
      }

      // show seconds even if equal 0 or no previous fields.
      if ( fields < maxFields ) {
	 strbuf.append( secs + " sec");
	 fields ++;
	 if ( secs == 1 )
	    strbuf.append( singular );
	 else
	    strbuf.append( plurar );
      }

      // remove the final ", " from the last field.
      if ( strbuf.length() >= 2 ) {
	 strbuf.deleteCharAt( strbuf.length()-1 );
	 strbuf.deleteCharAt( strbuf.length()-1 );
      }
      
      return strbuf.toString();
   }

   public static void main( String [] args )
   {
      System.out.println( TextUtils.fitRight( (1)+"", 2 ));
   }
}
