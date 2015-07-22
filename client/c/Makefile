
BUILD_ARCH=$(shell uname -m)
DEPLOYMENT_ARCHIVE=deploy
EXECUTABLES=ruftp-webcam-client

CPPFLAGS+=-D_GNU_SOURCE
CFLAGS+=-Wall -std=c99

ifdef DEBUG
CPPFLAGS+=-D_DEBUG
CFLAGS+=-g -O0
else
CPPFLAGS+=-DNDEBUG
CFLAGS+=-O3
endif

CPPFLAGS+=-I../../../libruftp/c
CPPFLAGS+=-I../../../libruftp/c/util

LDFLAGS=-L../../../libruftp/c
LDLIBS=-lruftp

ifdef DEBUG
# Just using zlib's crc32 function.
LDLIBS+=-lz
endif

ifeq ($(BUILD_ARCH),armv6l)
else
endif

all : $(EXECUTABLES)

ruftp-webcam-client : main.o 
	$(CC) -o $@ $^ $(LDFLAGS) $(LDLIBS)

############################################################################
# Unit tests

clean :
	rm -f $(EXECUTABLES) *.o ut-*

.PHONY : clean

