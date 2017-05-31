DISK = /dev/disk2

flash:
	# The compressed image file expands to over 4GB. This can cause problems
	# for the built-in tools on some platforms. If you are unable to unzip
	# the archive, or see a message stating that it's corrupt, use 7-Zip
	# (Windows) or The Unarchiver (Mac OS) instead.
	diskutil unmountDisk $(DISK)
	sudo dd bs=1m if=iot_rpi3.img of=$(DISK)

connect:
	adb connect Android.local

clean:
	cd robocar; ./gradlew clean
	cd mobile; ./gradlew clean

test:
	cd robocar; ./gradlew test
	cd mobile; ./gradlew :app:test

checkstyle:
	cd robocar; ./gradlew checkstyle
	cd mobile; ./gradlew checkstyle

build:
	cd robocar; ./gradlew assembleDebug
	cd mobile; ./gradlew assembleDebug

download:
	cd photos; adb shell ls /storage/emulated/0/Pictures/robocar/* | tr '\r' ' ' | xargs -n1 adb pull

remove:
	adb shell rm /storage/emulated/0/Pictures/robocar/*
