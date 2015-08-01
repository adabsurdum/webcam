/**
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

import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class Test {

	private void stream( String s ) {
		byte[] raw = s.getBytes();
		ByteArrayInputStream buffer
			= new ByteArrayInputStream( raw );
		DataInputStream i
			= new DataInputStream( buffer );
		int n = 3;
		try {
			while( n-- > 0 ) {
				int m = 3;
				i.reset();
				for( byte b : raw ) {
					System.out.printf( " %d", b );
					if( m-- < 1 )
						break;
				}
				System.out.println( "" );
			}
		} catch( IOException e ) {
			System.err.print( e );
		}
	}

	public static void main( String[] args ) {
		byte[] ba = new byte[1];
		ba[0] = (byte)0xff;
		int cached_height = ( (0x000000FF & ba[0]) + 1 ) * 8;
		System.out.printf( "%d\n", cached_height );
	}
}

