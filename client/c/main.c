
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include <stdbool.h>
#include <arpa/inet.h>
#include <getopt.h>
#include <err.h>

#ifdef _DEBUG
#include <zlib.h>
#endif
#include "socket.h"
#include "sockinit.h"
#include "client.h"

static char           _ipv4[17];
static unsigned short _port = 17071;
static unsigned short _mtu  = 512;
static int _retries   = 3;
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

	s = udp_init_socket(0,NULL /* any local address */);
	if( s < 0 )
		err( -1, "failed creating UDP socket" );

	server.sin_family      = AF_INET;
	server.sin_addr.s_addr = inet_addr(_ipv4);
	server.sin_port = htons( _port );

	datalen = ruftp_fetch( s, &server, _mtu, _retries, &data );
	if( datalen > 0 ) {
		fwrite( data, sizeof(uint8_t), datalen, stdout );
#ifdef _DEBUG
		fprintf( stderr, "crc32(image) = %08X\n",
			(uint32_t)crc32( crc32(0, NULL, 0 ), data, datalen ) );
#endif
 		free( data );
		exit_status = EXIT_SUCCESS;
	}

	return exit_status;
}

