
BUILD_ARCH=$(shell uname -m)
DEPLOYMENT_ARCHIVE=deploy
EXECUTABLES=ruftp-webcam-server ruftp-snow-server

# Options
TIMESTAMP=1

CPPFLAGS+=-DDEFAULT_VIDEO_WIDTH=320 -DDEFAULT_VIDEO_HEIGHT=240
CPPFLAGS+=-D_GNU_SOURCE
ifdef TIMESTAMP
CPPFLAGS+=-DHAVE_TIMESTAMP
endif

CFLAGS+=-Wall -std=c99

ifdef DEBUG
CPPFLAGS+=-D_DEBUG
CFLAGS+=-g -O0
else
CPPFLAGS+=-DNDEBUG
CFLAGS+=-O3
endif

CPPFLAGS+=-I../../libruftp/c
CPPFLAGS+=-I../../libruftp/c/util
CPPFLAGS+=-I../../librastfont
CPPFLAGS+=-I../../libgraphicsff

LDFLAGS=-L../../libruftp/c -L../../libgraphicsff
LDLIBS=-lpng -lruftp -lgraphicsff 

ifdef TIMESTAMP
OBJECTS=../../librastfont/monaco13.o
endif

ifdef DEBUG
# Just using zlib's crc32 function.
LDLIBS+=-lz
endif

ifeq ($(BUILD_ARCH),armv6l)
else
endif

all : $(EXECUTABLES)

main.o : main.c
	$(CC) -c -o $@ $(CPPFLAGS) $(CFLAGS) -I../../libvideo -DHAVE_VIDEO $<

snow.o : main.c
	$(CC) -c -o $@ $(CPPFLAGS) $(CFLAGS) $<

ruftp-webcam-server : main.o $(OBJECTS)
	$(CC) -o $@ $^ $(LDFLAGS) $(LDLIBS) -L../../libvideo -lvideo

ruftp-snow-server : snow.o $(OBJECTS)
	$(CC) -o $@ $^ $(LDFLAGS) $(LDLIBS)

clean :
	rm -f $(EXECUTABLES) *.o ut-*

.PHONY : clean

