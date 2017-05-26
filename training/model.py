"""

This script implements a CNN to train a car to drive autonomously.

"""

# See http://matplotlib.org/2.0.0rc1/_sources/faq/howto_faq.txt
import matplotlib
matplotlib.use('Agg')

import csv
import cv2
import matplotlib.pyplot as plt
import numpy as np
import tensorflow as tf
from keras import backend as K
from keras.layers import Flatten, Dense, Lambda, Cropping2D, Convolution2D
from keras.models import Sequential
from sklearn.model_selection import train_test_split
from sklearn.utils import shuffle

#
# Settings
#

# Sample training data:
# https://d17h27t6h515a5.cloudfront.net/topher/2016/December/584f6edd_data/data.zip
INPUT_DATA_FOLDER = 'data/'
INPUT_DATA_FILE = INPUT_DATA_FOLDER + 'driving_log.csv'
INPUT_WIDTH = 320
INPUT_HEIGHT = 160

# Resulting models
OUTPUT_MODEL_HDF5 = 'model.h5'  # HDF5 file
OUTPUT_MODEL_JSON = 'model.json'
OUTPUT_MODEL_TENSORFLOW = 'model.pb'  # Protobuf
OUTPUT_MODEL_CHART = 'model.png'

# Params to tweak
EPOCHS = 10
CORRECTION = 0.2  # Steering correction to augment data
TEST_SIZE = 0.2

#
# You shouldn't need to modify anything below this point
#

lines = []
print('Reading %s...' % INPUT_DATA_FILE)
with open(INPUT_DATA_FILE) as csv_file:
    reader = csv.reader(csv_file)
    for line in reader:
        # Ignore header
        if line[0].startswith('IMG/'):
            # Format is center,left,right,steering,throttle,brake,speed
            steering = float(line[3])

            # Augment the data right away so that it gets randomized
            lines.append({'path': line[0], 'steering': steering, 'flip': False})  # center
            lines.append({'path': line[1], 'steering': steering + CORRECTION, 'flip': False})  # left
            lines.append({'path': line[2], 'steering': steering - CORRECTION, 'flip': False})  # right

            # Augment data by flipping the images horizontally
            lines.append({'path': line[0], 'steering': steering, 'flip': True})  # center
            lines.append({'path': line[1], 'steering': steering + CORRECTION, 'flip': True})  # left
            lines.append({'path': line[2], 'steering': steering - CORRECTION, 'flip': True})  # right

train_samples, validation_samples = train_test_split(lines, test_size=TEST_SIZE)
print('Samples found: %d (training: %d, validation: %d).'
      % (len(lines), len(train_samples), len(validation_samples)))


#
# Generators
#


def read_image(path, flip):
    image = cv2.imread(INPUT_DATA_FOLDER + path.strip())
    if image is None:
        raise Exception('Failed to read: %s' % path)
    if flip:
        # 1 means flipping around y-axis
        image = cv2.flip(image, 1)
    if image is None:
        raise Exception('Failed to flip: %s' % path)
    return image


def read_measurement(measurement, flip):
    return -measurement if flip else measurement


def generator(samples, batch_size=32):
    total_samples = len(samples)
    while 1:  # Loop forever so the generator never terminates
        samples = shuffle(samples)
        for offset in range(0, total_samples, batch_size):
            images = []
            measurements = []
            batch_samples = samples[offset:offset + batch_size]
            for batch_sample in batch_samples:
                images.append(read_image(batch_sample['path'], batch_sample['flip']))
                measurements.append(read_measurement(batch_sample['steering'], batch_sample['flip']))
            # Convert data to numpy format
            X_train = np.array(images)
            y_train = np.array(measurements)
            yield shuffle(X_train, y_train)


train_generator = generator(train_samples, batch_size=32)
validation_generator = generator(validation_samples, batch_size=32)

#
# Network
#

print('Starting training...')
model = Sequential()

# Normalization - a potential optimization here is to reduce by half the
# size of the input images to 80x160 to make training faster
model.add(Lambda(lambda x: x / 255.0 - 0.5, input_shape=(INPUT_HEIGHT, INPUT_WIDTH, 3)))

# This crops:
# - 50 rows pixels from the top of the image
# - 20 rows pixels from the bottom of the image
# - 0 columns of pixels from the left of the image
# - 0 columns of pixels from the right of the image
model.add(Cropping2D(cropping=((50, 20), (0, 0))))

# NVIDIA architecture
# https://arxiv.org/pdf/1604.07316v1.pdf
# https://devblogs.nvidia.com/parallelforall/deep-learning-self-driving-cars/
model.add(Convolution2D(24, 5, 5, subsample=(2, 2), activation='relu'))
model.add(Convolution2D(36, 5, 5, subsample=(2, 2), activation='relu'))
model.add(Convolution2D(48, 5, 5, subsample=(2, 2), activation='relu'))
model.add(Convolution2D(64, 3, 3, activation='relu'))
model.add(Convolution2D(64, 3, 3, activation='relu'))
model.add(Flatten())
model.add(Dense(100))
model.add(Dense(50))
model.add(Dense(10))
model.add(Dense(1))

# Perform training
model.compile(loss='mse', optimizer='adam')
history_object = model.fit_generator(generator=train_generator,
                                     samples_per_epoch=len(train_samples),
                                     validation_data=validation_generator,
                                     nb_val_samples=len(validation_samples),
                                     nb_epoch=EPOCHS, verbose=1)

# Save model
model.save(OUTPUT_MODEL_HDF5)
with open(OUTPUT_MODEL_JSON, 'w') as output_json:
    output_json.write(model.to_json())

# Save TensorFlow model
tf.train.write_graph(K.get_session().graph.as_graph_def(),
                     logdir='.', name=OUTPUT_MODEL_TENSORFLOW, as_text=False)

#
# Visualize loss
#

# Plot the training and validation loss for each epoch
print('Generating loss chart...')
plt.plot(history_object.history['loss'])
plt.plot(history_object.history['val_loss'])
plt.title('model mean squared error loss')
plt.ylabel('mean squared error loss')
plt.xlabel('epoch')
plt.legend(['training set', 'validation set'], loc='upper right')
plt.savefig(OUTPUT_MODEL_CHART)

#
# Done
#

print('Done.')
