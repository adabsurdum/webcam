
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <stdbool.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <getopt.h>
#include <err.h>

#include <X11/Xlib.h>
#include <X11/keysym.h>
#include <X11/Xutil.h>

#include "socket.h"
#include "ccp.h"

#ifdef HAVE_EXTRAS
static int _verbosity = 0;
#endif


const long ALLEVENTS 
	= 0 // NoEventMask
	| KeyPressMask
	| KeyReleaseMask
	| ButtonPressMask
	| ButtonReleaseMask
	| EnterWindowMask
	| LeaveWindowMask
	| PointerMotionMask
	| PointerMotionHintMask
	| Button1MotionMask
	| Button2MotionMask
	| Button3MotionMask
	| Button4MotionMask
	| Button5MotionMask
	| ButtonMotionMask
	| KeymapStateMask
	| ExposureMask
	| VisibilityChangeMask
	| StructureNotifyMask
	| ResizeRedirectMask
	| SubstructureNotifyMask
	| SubstructureRedirectMask
	| FocusChangeMask
	| PropertyChangeMask
	| ColormapChangeMask
	| OwnerGrabButtonMask;


typedef struct _context {

	Display *display;
	int      screen;
	Window   win;
	GC       gc;

} context_t;

static context_t _cx;

/**
  * Following buffers are what are sent to X11 for rendering
  * via XPutImage.
  */
static XImage *_img         = NULL;
static unsigned char *_data = NULL;

#ifdef HAVE_EXTRAS

static int _afterFxn( Display *d ) {
	fprintf( stderr, "_afterFxn( %p )\n", d );
	return 0;
}

static int _errorHandler( Display *d, XErrorEvent *err ) {
	static char buf[1024];
	switch( err->error_code ) {
	case BadAccess:
	case BadAlloc:
	case BadAtom:
	case BadColor:
	case BadCursor:
	case BadDrawable:
	case BadFont:
	case BadGC:
	case BadIDChoice:
	case BadImplementation:
	case BadLength:
	case BadMatch :
	case BadName:
	case BadPixmap:
	case BadRequest:
	case BadValue:
	case BadWindow:
	default:;
	}
	XGetErrorText( d, err->error_code, buf, 1024 );
	fprintf( stderr, "%s (#%lu %u.%u)\n",
		buf,
		//err->type
		err->serial,
		err->request_code,
		err->minor_code);
	return 0;
}


static int _ioErrorHandler( Display *d ) {
	fprintf( stderr, "Fatal I/O or syscall error\n" );
	exit(-1);
}
#endif


static int _createImage( Display *d, int W, int H ) {

	XVisualInfo info = {
		.visual = NULL,
		.visualid = 0,
		.screen = _cx.screen,
		.depth = 1,
		.class = StaticGray,
		.red_mask = 0,
		.green_mask = 0,
		.blue_mask = 0,
		.colormap_size = 0,
		.bits_per_rgb = 0
	};
	int nmatched;

	memset( &info, 0, sizeof(info));

	XVisualInfo *matching
		= XGetVisualInfo( d, VisualNoMask, &info, &nmatched );

#ifdef HAVE_EXTRAS
	fprintf( stdout, "matched %d XVisualInfo\n", nmatched );
	if( _verbosity > 1 ) {
		for(i = 0; i < nmatched; i++ ) {
			XVisualInfo *vi = matching + i;
			fprintf( stdout,
				"%02d-----------------\n"
				"        .depth = %08X\n"
				"        .class = %d\n"
				"     .red_mask = %08X\n"
				"   .green_mask = %08X\n"
				"    .blue_mask = %08X\n"
				".colormap_size = %d\n"
				" .bits_per_rgb = %d\n",i,
				vi->depth,
				vi->class,
				vi->red_mask,
				vi->green_mask,
				vi->blue_mask,
				vi->colormap_size,
				vi->bits_per_rgb );
		}
	}
#endif

	if( matching ) {

		const size_t S
			= W * H * sizeof(unsigned int);
		_data = malloc( S );

		if( _data == NULL ) abort();

		/**
		  * Following actually ignores what XGetVisualInfo returned
		  * above and sets up the default that is prevalent on all
		  * modern hardware.
		  */
		_img = XCreateImage( d, 
				matching[1].visual,
				24,     // depth
				ZPixmap,// format
				0,      // offset
				(char*)_data,
				W, H,
				32,  // bitmap_pad 8, 16, or 32
				0 ); // bytes per line, inferred
		if( _img == NULL ) {
			fprintf( stderr, "XCreateImage failed. Aborting...\n" );
			free( _data );
			_data = NULL;
		}

		XFree( matching );
	}

	return _img == NULL ? -1 : 0;
}


static Bool _procEvent( XEvent *e ) {

	Bool proceed = True;

	switch( e->type ) {

	case KeyPress:
		switch( XLookupKeysym(&(e->xkey), 0) ) {
		case XK_Shift_L:
		case XK_Shift_R:
			break;
		case XK_Escape:
			proceed = False;
			break;
		case XK_g: // for Grab
			break;
		}
		break;

	case KeyRelease:
		switch( XLookupKeysym(&(e->xkey), 0) ) {
		case XK_Shift_L:
		case XK_Shift_R:
			break;
		}
		break;

#ifdef HAVE_EXTRAS
	case ButtonPress:
	case ButtonRelease:
	case MotionNotify:
		break;

	case ConfigureNotify:
		fprintf( stdout, "ConfigureNotify( %d, %d )\n",
			((XConfigureEvent*)e)->width,
			((XConfigureEvent*)e)->height );
		break;

	case ClientMessage: 
		if(*XGetAtomName( _cx.display, e->xclient.message_type) == *"WM_PROTOCOLS") {
			proceed = False;
		}
		break;

	case EnterNotify:
	case LeaveNotify:
	case FocusIn:
	case FocusOut:
	case KeymapNotify:
	case Expose:
	case GraphicsExpose:
	case NoExpose:
	case VisibilityNotify:
	case CreateNotify:
	case DestroyNotify:
	case UnmapNotify:
	case MapNotify:
	case ReparentNotify:
	case GravityNotify:
	case CirculateNotify:
	case PropertyNotify:
	case SelectionClear:
	case SelectionNotify:
	case ColormapNotify:
	case MappingNotify:

	////////////////////////////////////////////////////////////////////////
	// Requests which a simple app should rarely handle itself, if even
	// receive assuming event masks have been setup properly...

	case MapRequest:
	case ConfigureRequest:
	case ResizeRequest:
	case CirculateRequest:
	case SelectionRequest:
		break;
	default:
		fprintf( stderr, "unexpected event type:%08X\n", e->type );
#endif
	}
#ifdef _ECHO_EVENTS_TO_STDOUT
	fprintf( stdout, "EV\t%s\n", X11_EVENT_NAME[e->type] );
#endif
	return proceed;
}


static void _create_gui( const char *devname, int W, int H ) {

	Display *d = _cx.display;
	Window topwin;

	XEvent e;

#ifdef _DEBUG
#ifdef HAVE_EXTRAS
	XSetAfterFunction( d, _afterFxn );
#endif
	XSynchronize( d, 1 );
#endif

	printf("default depth = %d\n", DefaultDepth(d, _cx.screen ));

	_cx.win = XCreateSimpleWindow( d,
		DefaultRootWindow(d),
		0, 0,
		W, H,
		0, // no border
		BlackPixel( d, _cx.screen ), // border
		0xcccccc  );// background
	if( _cx.win == 0 ) return;

	topwin = _cx.win;

	_cx.gc = XCreateGC( d, topwin,
		0,      // mask of values
		NULL ); // array of values

	XStoreName( d, topwin, devname );

	XSelectInput( d, topwin,
		  KeyPressMask
		| KeyReleaseMask
		| ButtonPressMask
		| ButtonReleaseMask
		| EnterWindowMask
		| LeaveWindowMask
		| PointerMotionMask
		//| PointerMotionHintMask this will limit MotionNotifyEvents
		| Button1MotionMask
		| Button2MotionMask
		| Button3MotionMask
		| Button4MotionMask
		| Button5MotionMask
		| ButtonMotionMask
		| KeymapStateMask
		| ExposureMask
		| VisibilityChangeMask
		| StructureNotifyMask
		//| ResizeRedirectMask //exclusion precludes ResizeRequest events.
		//| SubstructureNotifyMask
		| SubstructureRedirectMask
		| FocusChangeMask
		| PropertyChangeMask
		| ColormapChangeMask
		| OwnerGrabButtonMask
		);

	XMapWindow( d, topwin ); // ...wait to for map confirmation
	XMapSubwindows( d, topwin );

	do {
		XNextEvent( d, &e );   // calls XFlush
	} while( e.type != MapNotify );
}


/***************************************************************************
  * Network
  */

/**
  * On startup fetch an image from the (commandline-specified) server.
  * That establishes the required window size. Thereafter, the window
  * can be reused on for subsequent frames.
  */

static char           _ipv4[17];
static unsigned short _port = 22222;
static unsigned short _mtu  = 512;
static int _retries = 3;
static int _verbosity = 1;

static const char *USAGE
	= "%s -i <ip>[%s] -p <port>[%d] -m <mtu>[%d] -r <retries>[%d]\n";

static void _print_usage( const char *exename, FILE *fp ) {
	fprintf( fp, USAGE, 
		exename,
		_ipv4,
		_port,
		_mtu,
		_retries );
}



int main( int argc, char *argv[] ) {

	int exit_status = EXIT_FAILURE;
	uint8_t *data = NULL;
	ssize_t datalen = 0;
	struct sockaddr_in server;
	int s;

	/**
	  * Required initializations.
	  */
	const char *LOCALHOST = "127.0.0.1";
	strcpy( _ipv4, LOCALHOST );

	/**
	  * Process options
	  */

	do {
		static const char *OPTIONS = "i:p:m:r:v:h?";
		const char c = getopt( argc, argv, OPTIONS );
		if( c < 0 ) break;

		switch(c) {

		case 'i':
			if( strlen( optarg ) > 15 ) {
				err( -1, "addr too long: %s", optarg );
			} else
				strncpy( _ipv4, optarg, 16 );
			break;

		case 'p':
			_port = atoi( optarg );
			break;

		case 'm':
			_mtu = atoi( optarg );
			break;

		case 'r':
			_retries = atoi( optarg );
			break;

		case 'v':
			_verbosity = atoi( optarg );
			break;
		case '?':
		case 'h':
			_print_usage( argv[0], stdout );
			exit(-1);
		default:
			printf( "error: unknown option: %c\n", c );
			_print_usage( argv[0], stdout );
			exit(-1);
		}

	} while(1);

	s = init_udp_socket(0);
	if( s < 0 )
		err( -1, "failed creating UDP socket" );

	server.sin_family      = AF_INET;
	server.sin_addr.s_addr = inet_addr(_ipv4);
	server.sin_port = htons( _port );
#if 0
	datalen = fetch( s, &server, _mtu, _retries, &data );
	if( datalen > 0 ) {
		fwrite( data, sizeof(uint8_t), datalen, stdout );
#ifdef _DEBUG
		fprintf( stderr, "crc16(image) = %04X\n", crc16( data, datalen ) );
#endif
 		free( data );
		exit_status = EXIT_SUCCESS;
	}

	if( true /* initial image acquired */ ) {
		warnx( "failed acquiring initial image. exiting..." );
		exit(-1);
	}
#endif
	/**
	  * Set up shared context.
	  */

	_cx.display  = XOpenDisplay( NULL );

	const int dummy_W = 320;
	const int dummy_H = 320;
	if( _cx.display ) {

		if( _createImage( _cx.display, dummy_W, dummy_H ) == 0 ) {

#ifdef HAVE_EXTRAS
			if( _verbosity > 0 )
				fprintf( stdout, 
					"Server vendor: %s\n"
					"   X Protocol: %d.%d Release 0x%08X\n",
					XServerVendor(_cx.display),
					XProtocolVersion(_cx.display),
					XProtocolRevision(_cx.display),
					XVendorRelease(_cx.display) );
#endif
			_cx.screen = DefaultScreen( _cx.display );

#ifdef HAVE_EXTRAS
			XSetErrorHandler( _errorHandler );
			XSetIOErrorHandler( _ioErrorHandler );
#endif
			_create_gui( "foo", dummy_W, dummy_H ); // runs an event loop

			/**
			  * Primary event loop.
			  */

			do {

				XEvent e;

				/**
				  * Process UI events
				  */

				if( XCheckMaskEvent( _cx.display, ALLEVENTS, &e ) ) {
					_procEvent( &e );
				}

				/**
				  * Process network events (receptions).
				  */

				if( true /*_request_pending && _request_complete()*/ ) {
					int err 
						= XPutImage( _cx.display, _cx.win, _cx.gc, _img, 
							0, 0,
							0, 0,
							dummy_W, dummy_H );

					if( Success != err ) {
						static char buf[512];
						XGetErrorText( _cx.display, err, buf, sizeof(buf) );
						puts( buf );
					}
				}

			} while( true );

			XDestroySubwindows( _cx.display, _cx.win );
			XDestroyWindow( _cx.display, _cx.win );

			if( _img )
				XDestroyImage( _img );
		}
		XCloseDisplay( _cx.display );
	}

	return 0;
}

