
package org.lotech;

import android.app.Activity;
import android.view.View;
//import android.widget.Button;
//import android.widget.ToggleButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

//import android.media.AudioManager;
//import android.media.AsyncPlayer;
//import android.media.MediaScannerConnection;
import android.preference.PreferenceManager;
import android.content.Intent;
import android.content.SharedPreferences;

import android.util.Log;

import android.os.Environment;
//import android.os.PowerManager;

//import android.net.Uri;
//import java.io.File;
//import java.io.FileOutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.content.Context;

import android.os.Bundle;
import android.os.AsyncTask;

//import java.net.ServerSocket;
//import java.net.Socket;
//import java.net.SocketException;

//import java.io.InputStream;
//import java.io.IOException;

import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;

// BOSnap
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
//import java.io.PrintStream;
//import java.io.FileOutputStream;
import java.io.IOException;

import java.util.BitSet;
//import java.util.Arrays;
import java.util.Random;
import java.util.zip.CRC32;

// EOSnap

/**
  * TODO: Be aware of availability of WiFi, in particular its loss.
  */
public class Succ extends Activity {

	static final String LOG_TAG = "SUCC";

	static final int DEFAULT_PORT = 17071;

	// UI References

	ImageView    cameraView;
	ProgressBar  progressBar;
	Fetcher      curReq;
	//ToggleButton armingSwitch;
	//Button       alarmTest;

	//AudioManager audio;
	//AsyncPlayer  player;
	//boolean      playingAlarm;
	//PowerManager.WakeLock wakeLock = null;

	private void initUIRefs() {
		cameraView  = (ImageView)findViewById(R.id.cameraDisplay);
		progressBar = (ProgressBar)findViewById(R.id.progressBar);
	}

/*
	private void logStorageState() {
		// String getStorageState(File path)

		File dataDir = Environment.getDataDirectory();
		File downDir = Environment.getDownloadCacheDirectory();
		File xtrnDir = Environment.getExternalStorageDirectory();
		File rootDir = Environment.getRootDirectory();
		File alarmsDir
			= Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_ALARMS );

		Log.d( LOG_TAG, "    Data: " + dataDir );
		Log.d( LOG_TAG, "Download: " + downDir );
		Log.d( LOG_TAG, "External: " + xtrnDir );
		Log.d( LOG_TAG, "    Root: " + rootDir );
		Log.d( LOG_TAG, "  Alarms: " + alarmsDir );
		Log.d( LOG_TAG, "External storage state: " + Environment.getExternalStorageState() );
		Log.d( LOG_TAG, "External storage removable: " + Environment.isExternalStorageRemovable() );
	}
*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity, menu);
		return super.onCreateOptionsMenu(menu);
	}


	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		int id = item.getItemId();
		Log.d(LOG_TAG, "onOptionsItemSelected("+id+")");
        // Display the fragment as the main content.
		switch( id ) {
		case R.id.action_settings:
			Intent intentSetPref
				= new Intent(getApplicationContext(), SettingsActivity.class );
			startActivityForResult( intentSetPref, 0 );
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult( requestCode, resultCode, data );

		SharedPreferences sharedPreferences
			= PreferenceManager.getDefaultSharedPreferences(this);
		String server = sharedPreferences.getString( "SERVER", "NA" );
		Toast t
			= Toast.makeText( getApplicationContext(), server, Toast.LENGTH_SHORT );
		t.show();
	}

    /**
	  * Called when the activity is first created. 
	  * This is where you should do all of your normal static set up: 
	  * create views, bind data to lists, etc. This method also provides you
	  * with a Bundle containing the activity's previously frozen state, if
	  * there was one.  Always followed by onStart().
	  */
    @Override
    protected void onCreate( Bundle savedInstanceState ) {

        super.onCreate(savedInstanceState);
		Log.d( LOG_TAG, "onCreate" );
/*
		audio = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		player = new AsyncPlayer( "Catmon" );
		playingAlarm = false; // just to be sure.

		PowerManager pm
			= (PowerManager)getSystemService( Context.POWER_SERVICE );
		wakeLock = pm.newWakeLock( 
			PowerManager.PARTIAL_WAKE_LOCK, 
			"Catmon Alarm" );
		wakeLock.setReferenceCounted( false );
*/
		// Setup up UI.

        setContentView(R.layout.main);
		initUIRefs();
		
		cameraView.setImageResource( R.drawable.ic_action_camera );

		// Check the connectivity situation and possibly disable the
		ConnectivityManager connMgr 
			= (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = connMgr.getActiveNetworkInfo();
    }

	/**
	  * Called after your activity has been stopped, prior to it being 
	  * started again. Always followed by onStart()
	  */
    @Override
    protected void onRestart() {
		super.onRestart();
		Log.d( LOG_TAG, "onRestart" );
	}

	/**
	  * Called when the activity is becoming visible to the user.
	  * Followed by onResume() if the activity comes to the foreground, 
	  * or onStop() if it becomes hidden.
	  */
    @Override
	protected void onStart() {
		super.onStart();
		Log.d( LOG_TAG, "onStart" );
	}

	/**
	  * Called when the activity will start interacting with the user. 
	  * At this point your activity is at the top of the activity stack, 
	  * with user input going to it.  Always followed by onPause().
	  */
    @Override
	protected void onResume() {
		super.onResume();
		Log.d( LOG_TAG, "onResume" );
	}

	/**
	  * Called when the system is about to start resuming a previous 
	  * activity.
	  * This is typically used to commit unsaved changes to persistent data,
	  * stop animations and other things that may be consuming CPU, etc. 
	  * Implementations of this method must be very quick because the next 
	  * activity will not be resumed until this method returns.
	  * Followed by either onResume() if the activity returns back to the 
	  * front, or onStop() if it becomes invisible to the user.
	  */
    @Override
	protected void onPause() {
		super.onPause();
		Log.d( LOG_TAG, "onPause" );
	}

	/**
	  * Called when the activity is no longer visible to the user, 
	  * because another activity has been resumed and is covering this one.
	  * This may happen either because a new activity is being started, an
	  * existing one is being brought in front of this one, or this one is
	  * being destroyed.  Followed by either onRestart() if this activity 
	  * is coming back to interact with the user, or onDestroy() if this 
	  * activity is going away.
	  */
    @Override
	protected void onStop() {
		super.onStop();
		Log.d( LOG_TAG, "onStop" );
	}

	/**
	  * The final call you receive before your activity is destroyed. 
	  * This can happen either because the activity is finishing (someone
	  * called finish() on it, or because the system is temporarily 
	  * destroying this instance of the activity to save space. 
	  * You can distinguish between these two scenarios with the 
	  * isFinishing() method.
	  */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d( LOG_TAG, "onDestroy" );
		/*if( playingAlarm ) {
			player.stop();
			playingAlarm = false;
		}*/
	}

	public void onImageTap( View btn ) {
		Log.d( LOG_TAG, "onImageTap" );
		if( curReq == null /* don't allow overlapping requests! */ ) {
			SharedPreferences sharedPreferences
				= PreferenceManager.getDefaultSharedPreferences(this);
			String server
				= sharedPreferences.getString( "SERVER", "NA" );
			InetAddress addr = null;
			try {
				addr = InetAddress.getByName( server );
			} catch( UnknownHostException e ) {
				Log.e( LOG_TAG, e.toString() );
				Toast.makeText( getApplicationContext(), e.toString(), Toast.LENGTH_LONG ).show();
				return;
			}

			if( addr != null ) {
				curReq = new Fetcher( addr, DEFAULT_PORT, Integer.valueOf(256).shortValue(), 3 );
				curReq.execute();
			} else {
				Toast.makeText( getApplicationContext(), "null ip", Toast.LENGTH_SHORT).show();
			}
		} else {
			curReq.cancel( true /* mayInterruptIfRunning */ );
		}
	}

	/**
	  * Worker to fetch a PNG from a Reliable UDP File Transport Protocol
	  * server.
	  *
	  * The three types used by an asynchronous task are the following:
	  * 1. Params, the type of the parameters sent to the task upon execution.
	  * 2. Progress, the type of the progress units published during the 
	  *  background computation.
	  * 3. Result, the type of the result of the background computation.
	  * To mark a type as unused, simply use the type Void.
	  *
	  * Threading rules
	  * There are a few threading rules that must be followed for this class
	  * to work properly:
	  * 1. The AsyncTask class must be loaded on the UI thread. This is done
	  *    automatically as of JELLY_BEAN. 
	  * 2. The task instance must be created on the UI thread.
	  * 3. execute(Params...) must be invoked on the UI thread.
	  * 4. Do not call onPreExecute(), onPostExecute(Result), 
	  *    doInBackground( Params...), onProgressUpdate(Progress...)
	  *    manually.
	  * 5. The task can be executed only once (an exception will be thrown
	  *    if a second execution is attempted.)
	  *
	  * Memory observability
	  * AsyncTask guarantees that all callback calls are synchronized in 
	  * such a way that the following operations are safe without explicit
	  * synchronizations.
	  * 1. Set member fields in the constructor or onPreExecute(), and refer
	  *    to them in doInBackground(Params...).
	  * 2. Set member fields in doInBackground(Params...), and refer to them
	  *    in onProgressUpdate(Progress...) and onPostExecute(Result).
	  */
	private class Fetcher extends AsyncTask<Void,Integer,byte[]> {

		// BOSnap

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
		private final static int TIMEOUT_MS       = 1000;
		private final static int MAX_PACKET_SIZE  = 512;

		/**
		  * State
		  */
		private InetAddress server;
		private int port;
		private DatagramSocket socket;

		/**
		  * These buffers are reused.
		  */

		byte[] incomingBuffer;
		ByteArrayInputStream  istream;
		DataInputStream dis;

		ByteArrayOutputStream ostream;
		DataOutputStream dos;
		short mtu;
		int retries;

		// EOSnap

		public Fetcher( InetAddress addr, int port, short mtu, int retries ) {
			// BOSnap
			this.server  = addr;
			this.port    = port;
			this.retries = retries;
			this.mtu     = mtu;

			try {
				this.socket = new DatagramSocket( 0 );
			}
			catch( SocketException e ) {
				Log.e( LOG_TAG, e.toString() );
			}

			// input buffering
			incomingBuffer = new byte[ MAX_PACKET_SIZE ];
			istream = new ByteArrayInputStream( incomingBuffer );
			dis = new DataInputStream( istream );

			// output buffering
			ostream	= new ByteArrayOutputStream( MAX_PACKET_SIZE );
			dos = new DataOutputStream( ostream );
			// EOSnap
		}

		// BOSnap
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
				Log.e( LOG_TAG, e.toString() );
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
				Log.e( LOG_TAG, e.toString() );
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
				Log.e( LOG_TAG, e.toString() );
			}
		}

		// EOSnap

		////////////////////////////////////////////////////////////////////

		/**
		  * Invoked on the UI thread before the task is executed. 
		  * This step is normally used to setup the task, for instance by 
		  * showing a progress bar in the user interface.
		  */
		protected void onPreExecute() {
			//exception = null; // clean earlier uses' troubles.
			progressBar.setProgress( 0 );
			progressBar.setVisibility( View.VISIBLE );
		}

		/**
		  * Invoked on the background thread immediately after 
		  * onPreExecute() finishes executing. This step is used to perform 
		  * background computation that can take a long time. The parameters
		  * of the asynchronous task are passed to this step. The result of
		  * the computation must be returned by this step and will be passed
		  * back to the last step. This step can also use publishProgress(
		  * Progress...) to publish one or more units of progress.
		  * These values are published on the UI thread, in the 
		  * onProgressUpdate(Progress...) step.
		  *
		  * To ensure that a task is cancelled as quickly as possible, you 
		  * should always check the return value of isCancelled() 
		  * periodically from doInBackground(Object[]), if possible (inside 
		  * a loop for instance.)
		  */
		protected byte[] doInBackground( Void... p ) {

			// On Android, at least, we must NOT reuse DatagramPacket.
			//DatagramPacket incomingPacket // TODO: Confirm we can reuse.
			//	= new DatagramPacket( incomingBuffer, incomingBuffer.length );

			boolean timed_out = false;

			// First DAT packet allows calculation of all these...
			int sizeof_object   = 0;
			int sizeof_fragment = 0;
			int total_size = 0;
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

			try {

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
						
						DatagramPacket incomingPacket
							= new DatagramPacket( incomingBuffer, incomingBuffer.length );
						socket.setSoTimeout( TIMEOUT_MS );
						socket.receive( incomingPacket );
						n = incomingPacket.getLength();

					} catch( SocketTimeoutException e ) {
						Log.i( LOG_TAG, 
							String.format( "Sent %s %.3fs ago with no response. Resending.",
							missing == null ? "GET" : "RES",
							(float)TIMEOUT_MS/1000.0 ) );
						timed_out = true;
						continue;
					} catch( IOException e ) {
						Log.e( LOG_TAG, e.toString() );
					}

					if( isCancelled() ) {
						object_buffer = null;
						Log.e( LOG_TAG, "cancelled" );
						break;
					}

					timed_out = false; // Reset on each reception.

					/**
					  * Current server only sends one packet type to client:
					  */

					if( SNAP_DAT != ( 0x000000FF & incomingBuffer[0] ) ) {
						Log.e( LOG_TAG, String.format( "unexpected packet type %02X", incomingBuffer[0] ) );
						break;
					}

					int flags = incomingBuffer[1];

					dis.reset();
					dis.skip(2); // packet type + flags

					int psz = dis.readShort();
					int rid = dis.readInt();
					int fid = dis.readInt();
					int tot = dis.readInt();

					dis.skip( psz );

					if( SIZEOF_DAT_FF + psz != n ) {
						Log.e( LOG_TAG, String.format(
							"packet %d truncated: expect %d, rcvd %d\n",
							fid, SIZEOF_DAT_FF + psz, n ) );
						continue;
						//break; // ...to allow an ACK to be sent anyway.
					}

					/**
					  * Verify the packet's integrity.
					  */

					if( CRC16.calc( incomingBuffer, n-2 ) != (0x0000FFFF & dis.readShort() ) ) {
						Log.e( LOG_TAG, String.format( "invalid checksum: packet %d\n", fid ) );
						break;
					}

					if( rid != RID ) {
						Log.e( LOG_TAG, String.format( "current request %08x, received (%08x)", RID, rid ) );
						break; // ...to allow an ACK to be sent anyway.
					}

					if( missing == null /* ...then this is the 1st DAT packet. */ ) {

						/**
						  * Parse header and setup state. Only need to parse the header
						  * of the first DAT packet; subsequent packets' headers should
						  * be IDENTICAL up to but NOT including the fragment field.
						  */

						sizeof_object = tot;
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
						missing.set( 0, fragment_count ); // set all bits to true
					}

					if( tot != sizeof_object ) {
						Log.e( LOG_TAG, String.format( "object size changed: expected %d, this packet has %d\n",
							sizeof_object, tot ) );
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
					publishProgress(
						fragment_count - missing.cardinality(),
						fragment_count );

					/**
					  * Is the shipment complete? Bail out, if sdos...
					  */

					if( missing.isEmpty() ) {
						sendACK( RID );
						break;
					}
				}
			}
			catch( IOException e ) {
				Log.e( LOG_TAG, e.toString() );
			}
			finally {
				// Could send ACK here, too
			}

			return object_buffer;
		}

		/**
		  * Invoked on the UI thread after a call to publishProgress(Progress...).
		  * The timing of the execution is undefined. This method is used to
		  * display any form of progress in the user interface while the
		  * background computation is still executing. For instance, it can
		  * be used to animate a progress bar or show logs in a text field.
		  */
		protected void onProgressUpdate( Integer... progress) {
			progressBar.setMax( progress[1].intValue() );
			progressBar.setProgress( progress[0].intValue() );
		}

		/**
		  * Invoked on the UI thread after the background computation 
		  * finishes. The result of the background computation is passed 
		  * to this step as a parameter.
		  *
		  * Convert the byte array to the image it contains, show it
		  * in the UI, and sound an alarm.
		  */
		protected void onPostExecute( byte[] pngBuffer ) {
			if( pngBuffer != null ) {
				CRC32 crc = new CRC32();
				crc.update( pngBuffer );
				Log.i( LOG_TAG, String.format( "CRC32=%08X", crc.getValue() & 0xFFFFFFFF ) );
				Bitmap bm = BitmapFactory.decodeByteArray( pngBuffer, 0, pngBuffer.length );
				if( bm != null ) {
					cameraView.setImageBitmap( bm );
					Log.d( LOG_TAG, "BitmapFactory.decodeByteArray SUCCEEDED" );
				} else {
					cameraView.setImageResource( R.drawable.colortest );
					Log.d( LOG_TAG, "BitmapFactory.decodeByteArray FAILED" );
				}
			} else {
				Log.d( LOG_TAG, "onPostExecute got nada" );
				cameraView.setImageResource( R.drawable.colortest );
			}
			progressBar.setVisibility( View.INVISIBLE );
			//evidence = pngBuffer;
			/*
			if( player != null ) {
				player.play( 
					Succ.this,
					Uri.fromFile( new File( ALARM_FILE ) ),
					true,
					AudioManager.STREAM_ALARM );
				playingAlarm = true;
			}
			*/
			// Finally remove the outer class' reference to this
			// since this can't be reused and it has served its purpose.
			curReq = null;
		}

		protected void onCancelled( byte[] pngBuffer ) {
			progressBar.setVisibility( View.INVISIBLE );
			curReq = null;
		}
	}
}

