# Hardware

Interested in building your own Android Robocar? These are some off-the-shelf components that we use to build our cars.

### Basic Robocar

This is what you need to put together a basic Robocar:

* Raspberry Pi, your Robocar brain.

  * Recommended: You might want to start with Adafruit's [Raspberry Pi 3 Board Pack for Android Things](https://www.adafruit.com/products/3292) ($59.95), that comes with an SD card to flash Android Things.

* Frame, your Robocar wheels.

  * Recommended: [Junior Runt Rover](https://www.servocity.com/junior) from Servocity ($27.99). It includes the motors and the wheels, but not the batteries.

  * Alternative: Any frames from Servocity are great. They have frames in many different sizes, materials, and motor power.

  * Alternative: We hear good things about [Sain Smart](https://www.sainsmart.com/arduino/arduino-kits/robotkits.html) and [Pololu](https://www.pololu.com/product/2509) frames, but we haven't tried them yet.

* Motor controller, to connect your motors to the Raspberry Pi.

  * Recommended: Adafruit's [DC & Stepper Motor HAT for Raspberry Pi](https://www.adafruit.com/products/2348) ($22.50). We've ported the Python library to Android so that you can control your motors from Java. **Note: some basic soldering required.**

  * Alternative: Adafruit's [16-Channel 12-bit PWM/Servo Driver - I2C interface - PCA9685](https://www.adafruit.com/product/815) ($14.95). **Note: some basic soldering required.**

  * Alternative: We hear good things about [Pololu](https://www.pololu.com/category/10/brushed-dc-motor-controllers) motor controllers, but we haven't tried them yet.

* Battery, to power both the Raspberry Pi and the motors.

  * Recommended: You could use a different battery pack for the motors and for the Raspberry Pi but we recommend using the same for both to make things simpler. For example, you could buy a portable charger like [this one](https://www.amazon.com/gp/product/B00Z9QVE4Q/) ($29.99) that supports two USB ports.

  * Alternative: A cheaper option is to simply use a 4xAA battery holder [like this one](https://www.adafruit.com/products/830) ($2.95) for the motors.

### Optional elements

* Controller. You can always control your car using the companion app without having to buy any more hardware. However, a Bluetooth controller is a great way to drive the Robocar too.

  * Recommended: The [NES30 Game Controller](http://amzn.to/2n2so4X) ($34.99) not only looks great, it supports both USB and Bluetooth (we support it out of the box too).

  * Alternative: There's a [Pro version](http://amzn.to/2rAvRwH) ($42.00) that comes with two joysticks if that's your thing.

* Camera, for lane detection (required for Computer Vision and Machine Learning autonomous driving). If you buy a camera, you might want to buy a [mount clamp](http://amzn.to/2rOfrBU) like this one ($14.59).

  * Recommended: We're currently using a [8 megapixels camera board](https://www.adafruit.com/products/3099) ($29.95) inside a [case with a 1/4" tripod mount](https://www.adafruit.com/products/3253) ($2.95).

* Portable router. During development, it's easier to connect both your laptop and the Robocar to the same wifi and use it to deploy app updates. If the router is portable, you can bring it with you to meetups without having to change the set up.

  * Recommended: The [HooToo TripMate Elite](http://amzn.to/2snnzX7) router is great, it performs well and can charge your devices.

* Crossover networking cable to connect the Raspberry Pi directly to your computer (useful during development).

  * Recommended: [Belkin CAT5e](http://amzn.to/2n2vnu7) ($4.80)

* Soldering station.

  * Recommended: [I really like this one](http://amzn.to/2nwGdLW) ($96.72).

---

Questions? Please [open a ticket](https://github.com/zugaldia/android-robocar/issues/new).
