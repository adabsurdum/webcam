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

import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;

import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
import java.io.PrintStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.BitSet;
import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

/**
  * Command line tool to fetch an image from a RUFTP server.
  */
class Grab {

	private final static int SNAP_BIT  = 0x80;
	private final static int SNAP_MASK = 0x07;

	private final static int SNAP_GET = SNAP_BIT|0x01; // c  => s  triggers wave of PKT_DAT s => c

	/**
	  * Server sends one data packet.
	  */
	private final static int SNAP_DAT = SNAP_BIT|0x02; // c <=  s

	/**
	  * Client request resending of a specified set of packet indices.
	  */
	private final static int SNAP_RES = SNAP_BIT|0x03; // c  => s  server responds with series PKT_DAT s => c

	/**
	  * Client notifies server that client has received all content.
	  * This is an optimization to allow the server to discard state associated
	  * with the last request "early." Server will discard it anyway after a
	  * timeout.
	  */
	private final static int SNAP_ACK = SNAP_BIT|0x04; // c  => s  client notifies server that PKT_DAT s => c

	/**
	  * Atomic protocol components, shared between multiple packets.
	  */
	private final static int SIZEOF_PACKET_TYPE = 1;
	private final static int SIZEOF_FLAGS       = 1;
	private final static int SIZEOF_FRAGMENT_ID = 4;
	private final static int SIZEOF_OBJECT_SIZE = 4; // TODO: Use Java's sizeof?
	private final static int SIZEOF_MTU         = 2;
	private final static int SIZEOF_REQUEST_ID  = 4;
	private final static int SIZEOF_CRC         = 2;
	private final static int SIZEOF_PAYLD_SIZE  = 2;

	/**
	  * OFFSET_x constants below describe octet offsets from start of UDP packet
	  * data to the specified item.
	  * SIZEOF_x_FF is the number of octets of Fixed Fields in each packet type.
	  * All packets in this protocol are composed of:
	  * 1) a fixed header and
	  * 2) a 2-octet CRC tail.
	  * All headers begin with a 1-octet packet type.
	  */
	private final static int OFFSET_GET_RID    = ((2*SIZEOF_FLAGS)+SIZEOF_PAYLD_SIZE);
	private final static int OFFSET_GET_MTU    = OFFSET_GET_RID + SIZEOF_REQUEST_ID;
	private final static int OFFSET_GET_REQ    = OFFSET_GET_MTU + SIZEOF_MTU;

	private final static int SIZEOF_GET_FF     = OFFSET_GET_REQ + SIZEOF_CRC;


	private final static int OFFSET_DAT_RID    = ((2*SIZEOF_FLAGS)+SIZEOF_PAYLD_SIZE);
	private final static int OFFSET_DAT_FID    = OFFSET_DAT_RID + SIZEOF_REQUEST_ID;
	private final static int OFFSET_DAT_SOO    = OFFSET_DAT_FID + SIZEOF_FRAGMENT_ID;
	private final static int OFFSET_DAT_PYLD   = OFFSET_DAT_SOO + SIZEOF_OBJECT_SIZE;

	private final static int SIZEOF_DAT_FF     = OFFSET_DAT_PYLD + SIZEOF_CRC;
	private final static int FLAG_TAIL_FRAG    = 0x01;

	private final static int OFFSET_RES_RID    = ((2*SIZEOF_FLAGS)+SIZEOF_PAYLD_SIZE);
	private final static int OFFSET_RES_1STFID = OFFSET_RES_RID + SIZEOF_REQUEST_ID;

	private final static int SIZEOF_RES_FF     = OFFSET_RES_1STFID + SIZEOF_CRC;


	private final static int OFFSET_ACK_RID    = SIZEOF_PACKET_TYPE;
	private final static int OFFSET_ACK_CRC    = OFFSET_ACK_RID + SIZEOF_REQUEST_ID;

	/**
	  * Miscellaneous other constants.
	  */
	private final static boolean DUMP_PACKETS = true;
	private final static int TIMEOUT_MS       = 3000;
	private final static int MAX_PACKET_SIZE  = 512;

	/**
	  * State
	  */
	private InetAddress server;
	private short port;
	private DatagramSocket socket;

	/**
	  * These buffers are reused.
	  */

	byte[] incomingBuffer;
	ByteArrayInputStream  istream;
	DataInputStream dis;

	ByteArrayOutputStream ostream;
	DataOutputStream dos;

	//BinaryDumper dumper = new BinaryDumper( System.out );

	public Grab( InetAddress addr, short port ) {
		this.server = addr;
		this.port   = port;
		try {
			this.socket = new DatagramSocket( 0 );
		}
		catch( SocketException e ) {
			System.err.println( e );
			System.exit(-1);
		}

		// input buffering
		incomingBuffer = new byte[ MAX_PACKET_SIZE ];
		istream = new ByteArrayInputStream( incomingBuffer );
		dis = new DataInputStream( istream );

		// output buffering
		ostream	= new ByteArrayOutputStream( MAX_PACKET_SIZE );
		dos = new DataOutputStream( ostream );

	}

	private static void dump( byte[] bytes, int len, PrintStream ps ) {
        for( byte b : bytes ) {
			ps.printf( " %02X", 0x000000FF & b );
			if( --len < 1 ) break;
		}
		ps.println( "" );
	}

	/**
	  * Build and send a GET packet to the server.
	  */
	private void sendGET( int rid, short mtu ) {

		try {

			ostream.reset();

			dos.writeByte( Integer.valueOf(SNAP_GET).byteValue() );
			dos.writeByte( 0  );
			dos.writeShort( 0 );
			dos.writeInt( rid );
			dos.writeShort( mtu );
			dos.writeShort( Integer.valueOf( CRC16.calc( ostream.toByteArray() ) ).shortValue() );

			//socket.setSoTimeout( 3000 );
			socket.send( new DatagramPacket( ostream.toByteArray(), ostream.size(), server, port ) );

		} catch( IOException e ) {
			System.err.println( e.toString() );
		}
	}


	/**
	  * Build and send as many RES packets as necessary (given the MTU) to
	  * inform the server to RESend missing packets.
	  */
	private void sendRES( int rid, int mtu, BitSet missing ) {

		final int MAX_PACKET_CAPACITY // max fragids per packet for the MTU
			= ( mtu - SIZEOF_RES_FF ) / SIZEOF_FRAGMENT_ID;
		int rem
			= missing.cardinality();
		final int PACKET_CAPACITY 
			= rem < MAX_PACKET_CAPACITY
			? rem
			: MAX_PACKET_CAPACITY;

		try {

			int fid = 0;
			while( rem > 0 ) {

				ostream.reset();

				int n = rem < PACKET_CAPACITY
					? rem
					: PACKET_CAPACITY;
				rem -= n;

				// Build the packet body.

				dos.writeByte( Integer.valueOf(SNAP_RES).byteValue() );
				dos.writeByte( 0 );
				dos.writeShort( n*4 );
				dos.writeInt( rid );

				while( n-- > 0 ) {
					fid = missing.nextSetBit(fid);
					if( fid < 0 )
						break;
					dos.writeInt( fid );
					fid += 1;
				}

				dos.writeShort( Integer.valueOf( CRC16.calc( ostream.toByteArray() ) ).shortValue() );

				//socket.setSoTimeout( 3000 );
				socket.send( new DatagramPacket( ostream.toByteArray(), ostream.size(), server, port ) );
			}

		} catch( IOException e ) {
			System.err.println( e.toString() );
		}
	}

	private void sendACK( int rid ) {

		try {

			ostream.reset();

			dos.writeByte( Integer.valueOf( SNAP_ACK ).byteValue() );
			dos.writeByte( 0 );
			dos.writeShort( 0 );
			dos.writeInt( rid );
			dos.writeShort( Integer.valueOf( CRC16.calc( ostream.toByteArray() ) ).shortValue() );

			//socket.setSoTimeout( 3000 );
			socket.send( new DatagramPacket( ostream.toByteArray(), ostream.size(), server, port ) );

		} catch( IOException e ) {
			System.err.println( e.toString() );
		}
	}

	/**
	  * This function fully implements a single "fetch" (GET) from a Grab server.
	  */
	public byte[] fetch( String uri, short mtu, int retries ) throws IOException {

		DatagramPacket incomingPacket // TODO: Confirm we can reuse.
			= new DatagramPacket( incomingBuffer, incomingBuffer.length );

		boolean timed_out = false;

		// First DAT packet allows calculation of all these...
		int sizeof_object   = 0;
		int sizeof_fragment = 0;
		int fragment_count = 0;
		byte[] object_buffer = null;
		BitSet missing = null;

		int n = 0; // used below EXCLUSIVELY for sizeof received packet.

		/**
		  * Generate a random request id, and insure it's not zeros.
		  */

		int RID = new Random().nextInt();

		/**
		  * 1. Send the GET request and wait a fixed amount of time for an
		  *    initial DAT packet response.
		  *    Resend the GET request up to <retries> times in the event of
		  *    timeout(s).
		  *    The initial DAT packet is effectively an ACK--says the server
		  *    heard us--and establishes the parameters of the response.
		  */

		while( true ) {

			/**
			  * Send a command...
			  */

			if( missing != null /* at least one DAT packet has been rcvd */ ) {
				if( timed_out ) {
					sendRES( RID, mtu, missing );
				}
			} else
			if( --retries > 0 )
				sendGET( RID, mtu );
			else
				return null;

			/**
			  * ...and await a response (which can only be DAT packets).
			  */

			try {
				
				socket.setSoTimeout( 3000 );
				socket.receive( incomingPacket );
				n = incomingPacket.getLength();

			} catch( SocketTimeoutException e ) {
				System.out.printf(
					"Sent %s %.3fs ago with no response. Resending.\n",
					missing == null ? "GET" : "RES",
					(float)TIMEOUT_MS/1000.0 );
				timed_out = true;
				continue;
			} catch( IOException e ) {
				System.err.println( e.toString() );
			}

			timed_out = false; // Reset on each reception.

			if( DUMP_PACKETS ) {
				System.out.printf( "Received packet(%d bytes):\n", incomingPacket.getLength() );
				dump( incomingBuffer, OFFSET_DAT_PYLD, System.out );
				//dumper.dump( incomingBuffer, 0, 9 );
			}

			/**
			  * Current server only sends one packet type to client:
			  */

			if( SNAP_DAT != ( 0x000000FF & incomingBuffer[0] ) ) {
				System.err.printf( "unexpected packet type %02X", incomingBuffer[0] );
				break;
			}

			int flags = incomingBuffer[1];

			dis.reset();
			dis.skip(2); // packet type + flags

			int psz = dis.readShort();
			int rid = dis.readInt();
			int soo = dis.readInt();
			int fid = dis.readInt();

			dis.skip( psz );

			/**
			  * Verify the packet's integrity.
			  */

			if( CRC16.calc( incomingBuffer, n-2 ) != (0x0000FFFF & dis.readShort() ) ) {
				System.err.printf( "invalid checksum: packet %d\n", fid );
				break; // ...to allow an ACK to be sent anyway.
			}

			if( rid != RID ) {
				System.err.printf( "current request %08x, received (%08x)", RID, rid );
				break; // ...to allow an ACK to be sent anyway.
			}

			if( missing == null /* ...then this is the 1st DAT packet. */ ) {

				/**
				  * Allocate a buffer for the object, and set up the
				  * bitvector that records which fragments we have received.
				  * Note that the 10 bytes of (packet_size,rid,object_size)
				  * should be identical in every received DAT packet.
				  */

				sizeof_object = soo;
				object_buffer = new byte[ sizeof_object ];

				/**
				  * Calculate the packetization.
				  * If this is the tail fragment then we know the fragment
				  * count and calculate sizeof_fragment from that; otherwise
				  * we know the sizeof_fragment and calculate fragment_count
				  * from that.
				  */

				if( (flags & FLAG_TAIL_FRAG) != 0 ) {

					fragment_count
						= fid+1;
					sizeof_fragment
						= (sizeof_object-psz) / fragment_count;

					assert ((sizeof_object-psz) % fragment_count) == 0;
					assert sizeof_fragment*fid + psz == sizeof_object;

				} else {

					sizeof_fragment
						= psz;
					fragment_count
						= sizeof_object / sizeof_fragment
						+ ( (sizeof_object % sizeof_fragment) > 0 ? 1 : 0);

					assert (fragment_count-1)*sizeof_fragment <= sizeof_object;
					assert sizeof_object <=  fragment_count*sizeof_fragment;
				}

				/**
				  * Allocate BitSet to monitor missing blocks.
				  */

				missing = new BitSet( fragment_count );
				missing.set( 0, fragment_count );
			}

			if( soo != sizeof_object ) {
				System.err.printf( "object size changed: expected %d, this packet has %d\n",
					sizeof_object, soo );
				break;
			}

			if( ! ( 0 <= fid && fid < fragment_count ) ) {
				throw new RuntimeException( String.format( "bad fid: %d", fid ) );
			}
			if( ! ( fid*sizeof_fragment < sizeof_object ) ) {
				throw new RuntimeException( String.format(
						"bad state:  fid: %d, byte-per-packet: %d, sizeof(object): %d",
						fid, sizeof_fragment, sizeof_object ) );
			}

			/**
			  * Copy object data verbatim into object buffer.
			  */

			System.arraycopy(
				incomingBuffer, OFFSET_DAT_PYLD,
				object_buffer, fid*sizeof_fragment,
				psz );

			missing.clear( fid );

			/**
			  * Is the shipment complete? Bail out, if sdos...
			  */

			if( missing.isEmpty() ) break;
		}

		sendACK( RID );

		return object_buffer;
	}

	public static void main( String[] args ) {

		InetAddress addr = null;
		short port = 17071;
		int retries = 3;
		short mtu = 512;
		int mult = 1;
		String server = "localhost";
		if( args.length < 1 ) {
			System.err.printf( "Grab <filename> [ <ip>=%s ] [ <port>=%d ] [ <mtu>=%d ] [ <retries>=%d ]\n",
				  server, port, mtu, retries );
			System.exit(-1);
		}

		if( args.length > 1 ) {
			server = args[1];
		}
		try {
			addr = InetAddress.getByName( server );
		} catch( UnknownHostException e ) {
			System.err.println( e );
			System.exit(-1);
		}

		if( args.length > 2 ) {
			port = Integer.decode(args[2]).shortValue();
		}
		if( args.length > 3 ) {
			mult = Integer.decode(args[3]).intValue();
		}
		if( args.length > 4 ) {
			retries = Integer.decode(args[4]).intValue();
		}

		Grab s = new Grab( addr, port );
		try {
			byte[] result = s.fetch( "", mtu, retries );
			if( result != null ) {
				CRC32 crc = new CRC32();
				crc.update( result );
				System.out.printf( "%08X\n", crc.getValue() & 0xFFFFFFFF );
				FileOutputStream f = new FileOutputStream( args[0] );
				f.write( result );
				f.close();
			} else {
				System.out.println( "No response. Gave up." );
			}
		} catch( IOException e ) {
			System.err.println( e );
			e.printStackTrace( System.err );
		}
	}
}

