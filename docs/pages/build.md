# Build your Robocar

Alright, things are getting fun now. Ready?

### Set up your frame

If you chose one from Servocity, they come with nice video instructions. For example, this video shows [how to set up the Junior Runt Rover](https://www.youtube.com/watch?v=hQCypE4pDBo).

![img_20170225_154112](https://cloud.githubusercontent.com/assets/6964/23334669/afefdc60-fb71-11e6-91ca-f8d1c32df33e.jpg)

### Attach your board

The simplest way is to use the snap mounts that come with the Servocity kit:

![img_20170810_213306](https://user-images.githubusercontent.com/6964/29198771-f50b9412-7e14-11e7-9815-1e4dbf212315.jpg)

If you bought the frame from another store, or prefer a different set up, some good old velcro between the components will do the trick too:

![img_20170810_213354](https://user-images.githubusercontent.com/6964/29198767-f506d468-7e14-11e7-864e-183185ccaafd.jpg)

### Optionally, attach your camera cable

You want to this now so that the camera cable gets properly inserted through the motor hat opening:

![img_20170810_213424](https://user-images.githubusercontent.com/6964/29198769-f5096430-7e14-11e7-9562-7b1269690b84.jpg)

![img_20170810_213446](https://user-images.githubusercontent.com/6964/29198770-f50ab09c-7e14-11e7-8f93-3e1fa9c5464f.jpg)

### Attach your motor hat

Next is to stack the motor hat on your board:

![img_20170810_213524](https://user-images.githubusercontent.com/6964/29198768-f5080e82-7e14-11e7-9ab2-517e599caa66.jpg)

Note that if you used a stacking header ([like this one](https://www.adafruit.com/product/1979)), you could stack multiple hats together. Some people like adding a sense hat, or a GPS hat.

### Connect the motors to the hat

The Servocity frame comes with all the required cables. You attach them to the hat like in the picture. Although you can change this in the software, the order that we follow is: 1) front-left wheel, 2) back-left wheel, 3) front-right wheel, and 4) back-right wheel:

![img_20170810_213631](https://user-images.githubusercontent.com/6964/29198774-f5141d76-7e14-11e7-8b6f-9431981e6b07.jpg)

### Connect the hat and the board to the power source

Use a regular USB cable for the board, and the serial cable for the motor hat.

### Optionally, set up the camera

If you want to use your car as a platform to test autonomous driving (computer vision and/or TensorFlow-powered machine learning) you need a camera. You want it high enough to have a good view of the tracks.

One option is using a regular camera clamp or a small tripod:

![img_20170810_214043](https://user-images.githubusercontent.com/6964/29198773-f513ea54-7e14-11e7-9280-b700e513f3fa.jpg)

![img_20170810_214121](https://user-images.githubusercontent.com/6964/29198772-f5127b4c-7e14-11e7-9b93-aab37ec2338e.jpg)


### Next: Install the software

[Continue reading.](pages/software)

---

Questions? Please [open a ticket](https://github.com/zugaldia/android-robocar/issues/new).
