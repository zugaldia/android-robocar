# Software

You're almost done.

The software for the project is a regular Android project. You can simply import the [`robocar`](https://github.com/zugaldia/android-robocar/tree/master/robocar) folder with Android Studio.

## Configuration

If you want to control your Robocar with a Bluetooth controller, and you probably want, there's one line of code that you need to manually change. You need to edit [`RobocarConstants`](https://github.com/zugaldia/android-robocar/blob/master/robocar/app/src/main/java/com/zugaldia/robocar/app/RobocarConstants.java) and set the address of your controller. You can obtain this value in a couple of ways:

- Connecting it to another device, like your laptop or phone, and obtaining the address from it.

- Turning the controller on before starting the Robocar. By default, if no controller is found to pair to, the Robocar will initiate Bluetooth discovery and print out available devices.

If you connect your controller via USB, there's nothing you need to change.

We're actively working to make this set up easier, [stay tuned](https://github.com/zugaldia/android-robocar/pull/41).

## Intallation

You can install the app like any other Android Things project.

- If you're using a board like the NXP Pico, you can simply attach the USB C cable to your laptop and use `adb` like you'd normally do with a regular Android device.

- If you're using a board like the Raspberry Pi, something we like is using a mobile router to set up a local network and have both your laptop running Studio and the Robocar connect to it. That way you can connect to the Robocar with `adb connect Android.local` and send your app wirelessly.

If you aren't familiar with the process, you can see detailed [instructions on the official documentation](https://developer.android.com/things/training/first-device/create-studio-project.html).

Once the app gets installed, your Robocar is ready to receive commands. Enjoy!


---

Questions? Please [open a ticket](https://github.com/zugaldia/android-robocar/issues/new).
