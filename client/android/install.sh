
#!/bin/bash
adb install -s bin/succ-debug.apk 
if [ -n "$1" ]; then
	adb logcat -c; adb logcat SUCC:D *:S | tee z
fi
