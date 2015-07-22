
/**
  * Java source to produce hexedit-like output
  */

import java.io.PrintStream;

public class HexDump {

	static final String[] padding = {
	"", "0", "00", "000", "0000", "00000", "000000", "0000000"
	};
	private byte[] buf;
	private int lpos = 0;
	private int addr = 0;
	private PrintStream out;

	public HexDump( PrintStream out ) {
		this.out = out;
		buf = new byte[8];
		addr = 0;
		lpos = 0;
	}

	static String pad( String num, int width ) {
		return( padding[ width - num.length() ] + num );
	}

	void dump( byte[] data, int offset, int end ) {
		do {
			// Move stuff to the line buffer...
			while( lpos < 8 && offset < end ) {
			buf[ lpos++ ] = data[ offset++ ];
			}
			// When the line buffer is full, format to output...
			if( 8 == lpos ) {
			out.print( pad( Integer.toHexString( addr ), 4 ) + ": " );
			for(int i = 0; i < 8; i++ ) {
			out.print( pad( Integer.toHexString( buf[i] ), 2 ) + " " );
			}
			out.println( new String( buf, 0, 8 ) );
			addr += 8;
			lpos = 0;
			}
		} while( offset < end );
	}

}

