package com.tigrisgame.awtui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.HeadlessException;
import java.awt.Toolkit;

/* File: UIScale.java
 * Description: Central UI scaling factor so the AWT GUI is usable on
 * HiDPI/4K displays.
 *
 * The scale is derived from the screen resolution relative to a
 * classic 1080p display, unless it is overridden with the system
 * property "tigris.scale" (e.g. -Dtigris.scale=2.5).
 */
public final class UIScale
{
    private static final float scale = computeScale();

    private UIScale()
    {
    }

    private static float computeScale()
    {
	// default: no scaling
	float computed = 1.0f;

	try {
	    String prop = System.getProperty( "tigris.scale" );
	    if ( prop != null ) {
		computed = Float.parseFloat( prop );
	    }
	    else {
		// scale relative to the smallest screen edge,
		// so a 1080p display => 1.0, 4K => 2.0.
		// round to the nearest 0.25
		Dimension screen =
		    Toolkit.getDefaultToolkit().getScreenSize();
		int ref = Math.min( screen.width, screen.height );

		computed =
		    Math.round( ( ref / 1080f ) * 4f ) / 4f;
	    }
	}
	// HeadlessException (running without a display) or a bad
	// property value: fall back to the default.
	catch ( HeadlessException he ) {
	    computed = 1.0f;
	}
	catch ( NumberFormatException nfe ) {
	    computed = 1.0f;
	}

	return Math.max( 1.0f, Math.min( 4.0f, computed ) );
    }

    /** the current UI scale factor. */
    public static float getScale()
    {
	return scale;
    }

    /** scale an integer pixel value or size. */
    public static int scale( int value )
    {
	return Math.round( value * scale );
    }

    /** scale a value given as a float. */
    public static int scale( float value )
    {
	return Math.round( value * scale );
    }

    /** convenient scaled font; size is the design-time (unscaled) size. */
    public static Font createFont( String name, int style, int size )
    {
	return new Font( name, style, scale( size ) );
    }
}
