import csv
import cv2
import numpy as np
from keras.models import Sequential
from keras.layers import Flatten, Dense, Lambda, Cropping2D, Convolution2D

DATA_FOLDER = 'data/'
CSV_FILE = DATA_FOLDER + 'driving_log.csv'

#
# Read CSV file
#

lines = []
print('Reading %s...' % CSV_FILE)
with open(CSV_FILE) as csv_file:
    reader = csv.reader(csv_file)
    for line in reader:
        if line[0].startswith('IMG/'):
            lines.append(line)

#
# Read training data
#

count = 0
correction = 0.2
images, measurements = [], []
for line in lines:
    count += 1
    # 0 center, 1 left, 2 right, 3 steering, 4 throttle, 5 brake, 6 speed
    print('[%d/%d] Reading %s...' % (count, len(lines), DATA_FOLDER + line[0]))
    images.extend([
        cv2.imread(DATA_FOLDER + line[0].strip()),  # center
        cv2.imread(DATA_FOLDER + line[1].strip()),  # left
        cv2.imread(DATA_FOLDER + line[2].strip())])  # right
    measurements.extend([
        float(line[3]),  # center
        float(line[3]) + correction,  # left
        float(line[3]) - correction])  # right

#
# Augment data by flipping the images horizontally
#

print('Augmenting data...')
augmented_images, augmented_measurements = [], []
for image, measurement in zip(images, measurements):
    augmented_images.append(image)
    augmented_images.append(cv2.flip(image, 1))
    augmented_measurements.append(measurement)
    augmented_measurements.append(measurement * -1.0)

#
# Get data in numpy format
#

assert len(augmented_images) == len(augmented_measurements)
print('Converting training %d elements...' % len(augmented_images))
X_train = np.array(augmented_images)
y_train = np.array(augmented_measurements)

#
# Network
#

print('Starting training...')
model = Sequential()

# Normalization
model.add(Lambda(lambda x: x / 255.0 - 0.5, input_shape=(160, 320, 3)))

# This crops:
# - 50 rows pixels from the top of the image
# - 20 rows pixels from the bottom of the image
# - 0 columns of pixels from the left of the image
# - 0 columns of pixels from the right of the image
model.add(Cropping2D(cropping=((50, 20), (0, 0)), input_shape=(160, 320, 3)))

# NVIDIA architecture
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

model.compile(loss='mse', optimizer='adam')
model.fit(X_train, y_train, validation_split=0.2, shuffle=True, nb_epoch=2)

model.save('model.h5')
print('Done.')
