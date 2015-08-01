
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

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

#include <getopt.h>
#include <err.h>

#include <X11/Xlib.h>
#include <X11/keysym.h>
#include <X11/Xutil.h>

#define BUFSIZE (VID_WIDTH * VID_HEIGHT)

struct video_format {
	unsigned width, height;
	char pixel_format[10];
} _fmt = {
	.width = VID_WIDTH,
	.height = VID_HEIGHT
};


int init_udp_socket(short port) {
	struct sockaddr_in addr;
	int s;
	s = socket(AF_INET, SOCK_DGRAM | SOCK_NONBLOCK, IPPROTO_UDP);
	if( s < 0 ) {
		return s;
	}
	addr.sin_family         = AF_INET;
	addr.sin_port           = htons(port);
	addr.sin_addr.s_addr    = INADDR_ANY;
	if( bind(s, (struct sockaddr *)&addr, sizeof(struct sockaddr))) {
		printf("Could not bind to UDP socket.\n");
		return -1;
	}
	return s;
}


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


static char           _ipv4[16];
static unsigned short _port = 22222;

static int                _sock;
static struct sockaddr_in _server;
//static uint8_t _buf[ VID_WIDTH * VID_HEIGHT ];
static uint8_t _buf[ VID_WIDTH + 1 ];

static void _fetch_and_render( void ) {

	socklen_t addrlen;
	int err, r, c, n, nr = _fmt.height;
	const char *REQ = "Hello";

	// Ping the server

	sendto( _sock, REQ, strlen(REQ), 0,
		  (struct sockaddr *)&_server, sizeof(_server));
#if 1
	while( nr-- > 0 ) {

		unsigned char *line;//_data
		const int EXPECT = _fmt.width+1;
		addrlen = sizeof(_server);
		n = recvfrom( _sock, _buf, BUFSIZE, 0, (struct sockaddr *)&_server, &addrlen);
		if( n < 0 )
			break;
		if( n != EXPECT ) {
			fprintf( stderr, "received %d bytes (%d too %s)\n",
				n,
				n > EXPECT ? n-EXPECT : EXPECT-n,
				n > EXPECT ? "many" : "few" );
			return;
		}

		// First byte is raster line
		r = *_buf;
		line = _data + 4*_fmt.width*r;
		// Remainder is one line of raster.
		for(c = 1; c <= _fmt.width; c++ ) {
			const uint8_t GRAY
				= (uint8_t)(0x00FF & _buf[ c ]);
			const int pixel = 4*c;
			line[ pixel+2 ] = GRAY; // red
			line[ pixel+1 ] = GRAY; // green
			line[ pixel+0 ] = GRAY; // blue
		}
	}
#else
	// 
	addrlen = sizeof(_server);
	n = recvfrom( _sock, _buf, BUFSIZE, 0, (struct sockaddr *)&_server, &addrlen);
	if( n != BUFSIZE ) {
		fprintf( stderr, "received %d bytes (%d too %s)\n",
			n,
			n > BUFSIZE ? n-BUFSIZE : BUFSIZE-n,
			n > BUFSIZE ? "many" : "few" );
		return;
	}

	/**
	  * Copy the video frame into our local buffer converting
	  * it from YUYV to grayscale rendered as 24-bit color
	  * in the process.
	  */

	for(r = 0; r < _fmt.height; r++ ) {
		for(c = 0; c < _fmt.width; c++ ) {
			const uint8_t GRAY
				= (uint8_t)(0x00FF & _buf[ r*_fmt.width + c ]);
			_data[ 4*_fmt.width*r + 4*c+2 ] = GRAY; // red
			_data[ 4*_fmt.width*r + 4*c+1 ] = GRAY; // green
			_data[ 4*_fmt.width*r + 4*c+0 ] = GRAY; // blue
		}
	}
#endif
	err = XPutImage( _cx.display, _cx.win, _cx.gc, _img, 
			0, 0,
			0, 0,
			_fmt.width, _fmt.height );

	if( Success != err ) {
		static char buf[512];
		XGetErrorText( _cx.display, err, buf, sizeof(buf) );
		puts( buf );
	}
}


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


static void _exec_gui( const char *devname, int W, int H ) {

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

	do {

		if( ! XCheckMaskEvent( d, ALLEVENTS, &e ) ) {

			_fetch_and_render();
		}

	} while( _procEvent( &e ) );

	XDestroySubwindows( d, topwin );
	XDestroyWindow( d, topwin );
}

static const char *USAGE
	= "%s -w <width> -h <height> -c <FOURCC pixel type> <device path>\n";

int main( int argc, char *argv[] ) {

	const char *LOCALHOST = "127.0.0.1";
	strcpy( _ipv4, LOCALHOST );
	/**
	  * Required initializations.
	  */

	memset( &_cx, 0, sizeof(_cx) );

	/**
	  * Process options
	  */

	do {
		static const char *OPTIONS = "w:h:c:i:p:v:";
		const char c = getopt( argc, argv, OPTIONS );
		if( c < 0 ) break;

		switch(c) {

		case 'w':
			_fmt.width = atoi( optarg );
			break;

		case 'h':
			_fmt.height = atoi( optarg );
			break;

		case 'c':
			strncpy( _fmt.pixel_format, optarg, 5 );
			break;

		case 'i':
			if( strlen( optarg ) > 15 ) {
				err( -1, "addr too long: %s", optarg );
			} else
				strncpy( _ipv4, optarg, 16 );
			break;

		case 'p':
			_port = atoi( optarg );
			break;

		case 'v':
#ifdef HAVE_EXTRAS
			_verbosity = atoi( optarg );
#endif
			break;
		default:
			printf ("error: unknown option: %c\n", c );
			exit(-1);
		}
	} while(1);

	/**
	  * Validate required arguments.
	  */

	if( optind < argc ) {
		//video_device
		//	= argv[ optind++ ];

	} else {
		//fprintf( stdout, USAGE, argv[0] );
		//abort();
	}

	_sock = init_udp_socket(0);
	if( _sock < 0 )
		err( -1, "failed creating UDP socket" );

	_server.sin_family      = AF_INET;
	_server.sin_addr.s_addr = inet_addr(_ipv4);
	_server.sin_port = htons( _port );

	/**
	  * Set up shared context.
	  */

	_cx.display  = XOpenDisplay( NULL );

	if( _cx.display ) {

		if( _createImage( _cx.display, _fmt.width, _fmt.height ) == 0 ) {

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
			_exec_gui( "foo", _fmt.width, _fmt.height ); // runs an event loop

			if( _img )
				XDestroyImage( _img );
		}
		XCloseDisplay( _cx.display );
	}

	return 0;
}

