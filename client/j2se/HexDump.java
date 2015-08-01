
/**
  * Java source to produce hexedit-like output
  *
  * Copyright (C) 2015  Roger Kramer
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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

