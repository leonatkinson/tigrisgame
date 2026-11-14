/* File: ArrayUtils.java
 * Description:  Util class for dealing with Java arrays
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.util;

import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.Random;

public class ArrayUtils 
{
   public static Random rand = new Random();    
   public static DecimalFormat doubleFormat;

   static {
      doubleFormat = new DecimalFormat();
      doubleFormat.setMinimumFractionDigits(3);
      doubleFormat.setMaximumFractionDigits(3);
      doubleFormat.setMinimumIntegerDigits(1);
      doubleFormat.setMaximumIntegerDigits(1);
   }

    // does NOT effect the input array.
    public static Object [] randomize( Object [] list  )
    {
	Object [] copyArray = (Object []) list.clone();
	
	// New way adapted from C++ STL implementation.
	// this algorithm is cute, and you can prove that
	// all elements have equal probability to be in 
	// any position by induction.
	for ( int ii = 1; ii<copyArray.length; ii++ ) {
	    // want to swap element ii with a
	    //  random index from 0 to ii.
	    int swapIndex = (int) (ii * rand.nextDouble());

	    if ( swapIndex != ii ) {
		Object tmp = copyArray[ ii ];
		copyArray[ ii ] = copyArray[ swapIndex ];
		copyArray[ swapIndex ] = tmp;
	    }
	}

	return copyArray;
    }

    // removed because sucked, and needed java2
//     public static Object [] randomize_OLD( Object [] list  )

    // does NOT effect the input array. 
    public static Object [] trimToSize( Object [] list, int size )
    {
	int trimSize = Math.min( list.length, size );
	Class elementClass = list.getClass().getComponentType();
	Object [] returnArray = (Object []) 
	    Array.newInstance( elementClass, trimSize );

	System.arraycopy( list, 0, returnArray, 0, trimSize );

	return returnArray;
    }

    // effects the passed in array IN PLACE.
    public static void sortAscending( Object [] array, Sorter sorter ) 
    {
	// this is a really dumb sort that goes through
	// the remainder of the list each time and finds the
	// best comparision and places it in the next spot.
	for ( int ii = 0; ii < array.length; ii++ ) {
	    int bestComp = ii;
	    for ( int jj = ii; jj < array.length; jj++ ) {
		if ( sorter.lteq( array[jj], array[bestComp] ) )
		    {
			bestComp = jj;
		    }
	    }

	    Object temp = array[ii];
	    array[ii] = array[bestComp];
	    array[bestComp] = temp;
	}
    }    

    // effects the passed in array IN PLACE.
    public static void sortDescending( Object [] array, Sorter sorter ) 
    {
	// this is a really dumb sort that goes through
	// the remainder of the list each time and finds the
	// best comparision and places it in the next spot.
	for ( int ii = 0; ii < array.length; ii++ ) {
	    int worstComp = ii;
	    for ( int jj = ii; jj < array.length; jj++ ) {
		if ( sorter.lteq( array[worstComp], array[jj] ) )
		    {
			worstComp = jj;
		    }
	    }

	    Object temp = array[ii];
	    array[ii] = array[worstComp];
	    array[worstComp] = temp;
	}
    }    

    // handles NULL and 0 SIZE arrays on either or both arrays.
    public static Object [] mergeArrays( Object[] array1, Object [] array2 )
    {
	Object [][] theArrays;
	array1 = (array1 == null) ? new Object[0] : array1;
	array2 = (array2 == null) ? new Object[0] : array2;

	if ( array1.length != 0 )
	    return mergeArrays( new Object[][] { array1, array2 } );
	else if ( array2.length != 0 )
	    return mergeArrays( new Object[][] { array2, array1 } );
	else 
	    // return a 0x0 size array of the right type.
	    return (Object[]) Array.newInstance( array1.getClass().
						 getComponentType(), 0 );   
    }

    // does NOT effect the input array. 
    public static Object [] mergeArrays( Object[][] theArrays )
    {
	if ( theArrays == null ||
	     theArrays.length == 0 )
	    return new Object[0];

	Class elementClass = theArrays[0].getClass().getComponentType();
	int size = 0;
	for ( int ii=0; ii<theArrays.length; ii++)
	    size += theArrays[ii].length;

	Object [] returnArray = (Object []) Array.newInstance( elementClass,
							       size );
	int indexFilled = 0;
	for ( int ii=0; ii<theArrays.length; ii++) {
	    System.arraycopy( theArrays[ii], 0, returnArray, 
			      indexFilled, theArrays[ii].length );
	    indexFilled += theArrays[ii].length;
	}
	
	return returnArray;
    }

    // uses .equals() method for checking.
    public static boolean contains( Object obj, Object [] array )
    {
	if ( array == null || obj == null )
	    return false;

	for ( int ii=0; ii< array.length; ii++)
	    {
		if ( obj.equals( array[ii] ) )
		    return true;
	    }
	return false;
    }


    public static int indexOf( int val, int[] array )
    {
	if ( array == null )
	    return -1;

	for ( int ii=0; ii<array.length; ii++ ) {
	    if ( array[ii] == val )
		return ii;
	}
	return -1;
    }

    public static void fill( double [] array, double val )
    {
	// if JDK1.2 was assumed, the java.util.Arrays fill method would work.
	for ( int ii=0; ii<array.length; ii++) { 
	    array[ii] = val;
	}
    }

   public static int[] sum( int [] a, int [] b )
   {
      int [] c = new int[ a.length ];
      for ( int ii=0; ii<c.length; ii++) {
	 c[ii] = a[ii] + b[ii];
      }
      return c;
   }

    /* BUBBLE SORT

	for (int i=population ; i < population*2 ; i++) {
	    for (int j = i ; j > 0 ; j--) {
		Double current = (Double)fitness.elementAt(j);
		Double prev = (Double)fitness.elementAt(j-1);
		if (current.doubleValue() >= prev.doubleValue()) {
		    // swap positions
		    double[] temp = (double[]) member.elementAt(j-1);
		    member.setElementAt(member.elementAt(j),j-1);
		    member.setElementAt(temp,j);
		    fitness.setElementAt(current,j-1);		    
		    fitness.setElementAt(prev,j);
		} else break;
	    }
	}
     */

    // TODO... we could probably pass in an Object 
    // (instead of double[] or something)
    // and use reflection to figure out what the hell to print.

    public static String prettyPrint( boolean [] array )
    {
	if ( array == null )
	    return prettyPrint( (Object[]) null );
	Object [] objArray = new Object[ array.length ];
	for ( int ii=0; ii<array.length; ii++)
	    objArray[ii] = new Boolean( array[ii] );
	return prettyPrint( objArray );
    }

    public static String prettyPrint( short [] array )
    {
	if ( array == null )
	    return prettyPrint( (Object[]) null );
	Object [] objArray = new Object[ array.length ];
	for ( int ii=0; ii<array.length; ii++)
	    objArray[ii] = new Short( array[ii] );
	return prettyPrint( objArray );
    }

    public static String prettyPrint( byte [] array )
    {
	if ( array == null )
	    return prettyPrint( (Object[]) null );
	Object [] objArray = new Object[ array.length ];
	for ( int ii=0; ii<array.length; ii++)
	    objArray[ii] = new Byte( array[ii] );
	return prettyPrint( objArray );
    }

    public static String prettyPrint( double [] array )
    {
	if ( array == null )
	    return prettyPrint( (Object[]) null );
	Object [] objArray = new Object[ array.length ];
	for ( int ii=0; ii<array.length; ii++)
	    objArray[ii] = new Double( array[ii] );
	return prettyPrint( objArray );
    }

    public static String prettyPrint( int [] array )
    {
	if ( array == null )
	    return prettyPrint( (Object[]) null );
	Object [] objArray = new Object[ array.length ];
	for ( int ii=0; ii<array.length; ii++)
	    objArray[ii] = new Integer( array[ii] );
	return prettyPrint( objArray );
    }

    public static String prettyPrint( long [] array )
    {
	if ( array == null )
	    return prettyPrint( (Long[]) null );
	Object [] objArray = new Object[ array.length ];
	for ( int ii=0; ii<array.length; ii++)
	    objArray[ii] = new Long( array[ii] );
	return prettyPrint( objArray );
    }

    public static String prettyPrint( Object [] array )
    {
	if ( array == null )
	    return "[null array]";
	if ( array.length == 0 )
	    return "[empty array]";

	StringBuffer buff = new StringBuffer("[");
	for( int ii=0; ii<array.length; ii++ ) {
	   if ( ii != 0 ) { buff.append( ", "); }

	   if ( array[ii] instanceof Double ) {
	      buff.append( doubleFormat.
			   format( ((Double)array[ii]).doubleValue()));
	   }
	   else {
	      buff.append( array[ii] );
	   }
	}
	buff.append( "]");
	return buff.toString();
    }

    public interface Sorter
    {
	public boolean lteq( Object obj1, Object obj2 );
    }
}
