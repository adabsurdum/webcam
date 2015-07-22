
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

