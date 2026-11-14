/* File: ResourceLoader.java
 * Description:  Utility to load images, etc from jar files.
 * Copyright (c) 2000-2007, Craig Moffitt.  All Rights Reserved.
 * Licensed under the Academic Free License ("AFL") v. 3.0;
 * you may not use this file except in compliance with the License.
 * You should have received a copy of the License with this program;
 * you may alternatively obtain a copy online at:
 * http://www.opensource.org/licenses/afl-3.0.php
 * */

package com.tigrisgame.util;

import java.io.*;
import java.util.Hashtable;

public class ResourceLoader
{
    public static int verbosity = 1;
    public static Hashtable cache = new Hashtable();

    public static byte [] loadResource( String imageLocation )
    {
	Object cacheObj = cache.get( imageLocation );
	if ( cacheObj != null ) {
	    return (byte []) cacheObj;
	}

	if ( verbosity > 1 )
	    System.out.println("attempting to load " + imageLocation );

	InputStream in = ResourceLoader.class.
	    getResourceAsStream( imageLocation );
	ByteArrayOutputStream outArray = new ByteArrayOutputStream();

	if ( in == null ) {
	    if ( verbosity > 0 )
		System.out.println("failed to load " + imageLocation +
				   ", null resource" );
	    return null;
	} 
	
	try {
	    byte [] tmpBuffer = new byte[ 512 ];
	    int bytesRead = 0;
	    int bytesTotal = 0;

	    while ( bytesRead != -1 ) {
		// copy first into output, so when we 
		// do get -1, it won't bomb on out of bounds.
		// 0 case is okay b/c 0 bytes are copied.
		outArray.write( tmpBuffer, 0, bytesRead );
		bytesRead = in.read( tmpBuffer, 0, 
				     tmpBuffer.length );
		bytesTotal += bytesRead;
	    }
	    
	    if ( verbosity > 1 )
		System.out.println("read " + bytesTotal + " bytes");

	    // save stored bytes.
	    tmpBuffer = outArray.toByteArray();

	    // cleanup.
	    in.close();
	    outArray.close(); 
	    in = null;
	    outArray = null;

	    cache.put( imageLocation, tmpBuffer );

	    return ( tmpBuffer );
	} catch (IOException ioe) {
	    if ( verbosity > 0 )
		System.out.println("failed to load " + imageLocation +
				   ", " + ioe.getMessage() );
	} 
// 	catch (IndexOutOfBoundsException bad ) {
// 	    bad.printStackTrace();
// 	    throw new RuntimeException(e);
// 	}

	return null;
    }

    // Uses Java serialization to read a file to disk.
    // returns null on failure, the object on success.
    public static Object fileLoadObject( String filename )
    {
    	try {
	    ObjectInputStream in =
		new ObjectInputStream( new FileInputStream( filename ));
	    Object obj = in.readObject();
	    in.close();

	    return obj;

	} catch ( InvalidClassException ice ) {
	    System.out.println("Warning: Unable to load object from file " 
			       + filename + ": " + ice ); 
	} catch ( ClassNotFoundException cnfe ) {
	    System.out.println("Warning: Unable to load object from file " 
			       + filename + ": " + cnfe ); 
	} catch( IOException ioe ) {
	    System.out.println("Warning: Unable to load object from file " 
			       + filename + ": " + ioe ); 
	}
	return null;
    }

    // Uses Java serialization to save a file to disk.
    // returns false on failure, true on success.
    public static boolean fileSaveObject( Object obj, String filename )
    {
	try {
	    ObjectOutputStream out =
		new ObjectOutputStream( new FileOutputStream( filename ));
	    out.writeObject( obj );
	    out.close();
	    return true;
 	} catch( IOException e ) {
 	    System.out.println("Warning: Unable to save game to file "
			       + filename + ": " + e ); 
 	}

	return false;
    }
}
