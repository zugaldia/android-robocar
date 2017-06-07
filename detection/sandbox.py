import glob
import time

import cv2
import matplotlib.image as mpimg
import matplotlib.pyplot as plt
import numpy as np
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler
from sklearn.svm import LinearSVC

from libs.car_detection import CarDetection

#
# Manual Vehicle Detection
#

image = mpimg.imread('input_images/bbox-example-image.jpg')
bboxes = [((275, 572), (380, 510)), ((488, 563), (549, 518)), ((554, 543), (582, 522)),
          ((601, 555), (646, 522)), ((657, 545), (685, 517)), ((849, 678), (1135, 512))]
result = CarDetection.draw_boxes(image, bboxes)
mpimg.imsave('output_images/test-draw_boxes.jpg', result)

#
# Template Matching
#


image = mpimg.imread('input_images/bbox-example-image.jpg')
templist = ['input_images/cutouts/cutout1.jpg', 'input_images/cutouts/cutout2.jpg',
            'input_images/cutouts/cutout3.jpg', 'input_images/cutouts/cutout4.jpg',
            'input_images/cutouts/cutout5.jpg', 'input_images/cutouts/cutout6.jpg']
bboxes = CarDetection.find_matches(image, templist)
print('find_matches: %s boxes found' % len(bboxes))
result = CarDetection.draw_boxes(image, bboxes)
mpimg.imsave('output_images/test-find_matches.jpg', result)

#
# Histograms of Color
#


image = mpimg.imread('input_images/cutouts/cutout1.jpg')
rh, gh, bh, bincen, feature_vec = CarDetection.color_hist(image, nbins=32, bins_range=(0, 256))
if rh is not None:
    # Plot a figure with all three bar charts
    fig = plt.figure(figsize=(12, 3))
    plt.subplot(131)
    plt.bar(bincen, rh[0])
    plt.xlim(0, 256)
    plt.title('R Histogram')
    plt.subplot(132)
    plt.bar(bincen, gh[0])
    plt.xlim(0, 256)
    plt.title('G Histogram')
    plt.subplot(133)
    plt.bar(bincen, bh[0])
    plt.xlim(0, 256)
    plt.title('B Histogram')
    fig.tight_layout()
    plt.savefig('output_images/test-color_hist.jpg')
    plt.close()
else:
    print('color_hist is returning None for at least one variable.')

#
# Spatial Binning of Color
#


# Read in an image
image = mpimg.imread('input_images/cutouts/cutout1.jpg')
feature_vec = CarDetection.bin_spatial(image, color_space='RGB', size=(32, 32))

# Plot features
plt.plot(feature_vec)
plt.title('Spatially Binned Features')
plt.savefig('output_images/test-bin_spatial.jpg')
plt.close()

#
# Data Exploration
#


# images are divided up into vehicles and non-vehicles
images = glob.glob('input_images/data_small/non-vehicles_smallset/notcars1/*jpeg') + \
         glob.glob('input_images/data_small/non-vehicles_smallset/notcars2/*jpeg') + \
         glob.glob('input_images/data_small/non-vehicles_smallset/notcars3/*jpeg') + \
         glob.glob('input_images/data_small/vehicles_smallset/cars1/*jpeg') + \
         glob.glob('input_images/data_small/vehicles_smallset/cars2/*jpeg') + \
         glob.glob('input_images/data_small/vehicles_smallset/cars3/*jpeg')
cars = []
notcars = []
for image in images:
    if 'image' in image.replace('input_images', '') or 'extra' in image:
        notcars.append(image)
    else:
        cars.append(image)
data_info = CarDetection.data_look(cars, notcars)
print('Your function returned a count of',
      data_info["n_cars"], ' cars and',
      data_info["n_notcars"], ' non-cars')
print('of size: ', data_info["image_shape"], ' and data type:',
      data_info["data_type"])
# Just for fun choose random car / not-car indices and plot example images
car_ind = np.random.randint(0, len(cars))
notcar_ind = np.random.randint(0, len(notcars))

# Read in car / not-car images
car_image = mpimg.imread(cars[car_ind])
notcar_image = mpimg.imread(notcars[notcar_ind])

# Plot the examples
fig = plt.figure()
plt.subplot(121)
plt.imshow(car_image)
plt.title('Example Car Image')
plt.subplot(122)
plt.imshow(notcar_image)
plt.title('Example Not-car Image')
plt.savefig('output_images/test-data_look.jpg')
plt.close()

#
# HOG
#

# Generate a random index to look at a car image
ind = np.random.randint(0, len(cars))
# Read in the image
image = mpimg.imread(cars[ind])
gray = cv2.cvtColor(image, cv2.COLOR_RGB2GRAY)
# Define HOG parameters
orient = 9
pix_per_cell = 8
cell_per_block = 2
# Call our function with vis=True to see an image output
features, hog_image = CarDetection.get_hog_features(gray, orient,
                                                    pix_per_cell, cell_per_block,
                                                    vis=True, feature_vec=False)

# Plot the examples
fig = plt.figure()
plt.subplot(121)
plt.imshow(image, cmap='gray')
plt.title('Example Car Image')
plt.subplot(122)
plt.imshow(hog_image, cmap='gray')
plt.title('HOG Visualization')
plt.savefig('output_images/test-get_hog_features.jpg')
plt.close()

#
# Combine and Normalize Features
#


car_features = CarDetection.extract_features(cars, cspace='RGB', spatial_size=(32, 32),
                                             hist_bins=32, hist_range=(0, 256))
notcar_features = CarDetection.extract_features(notcars, cspace='RGB', spatial_size=(32, 32),
                                                hist_bins=32, hist_range=(0, 256))

if len(car_features) > 0:
    # Create an array stack of feature vectors
    X = np.vstack((car_features, notcar_features)).astype(np.float64)
    # Fit a per-column scaler
    X_scaler = StandardScaler().fit(X)
    # Apply the scaler to X
    scaled_X = X_scaler.transform(X)
    car_ind = np.random.randint(0, len(cars))
    # Plot an example of raw and scaled features
    fig = plt.figure(figsize=(12, 4))
    plt.subplot(131)
    plt.imshow(mpimg.imread(cars[car_ind]))
    plt.title('Original Image')
    plt.subplot(132)
    plt.plot(X[car_ind])
    plt.title('Raw Features')
    plt.subplot(133)
    plt.plot(scaled_X[car_ind])
    plt.title('Normalized Features')
    fig.tight_layout()
    plt.savefig('output_images/test-extract_features.jpg')
    plt.close()
else:
    print('extract_features only returns empty feature vectors.')

#
# Color Classify
#


# Play with these values to see how your classifier
# performs under different binning scenarios
spatial = 32
histbin = 32

car_features = CarDetection.extract_features(cars, cspace='RGB', spatial_size=(spatial, spatial),
                                             hist_bins=histbin, hist_range=(0, 256))
notcar_features = CarDetection.extract_features(notcars, cspace='RGB', spatial_size=(spatial, spatial),
                                                hist_bins=histbin, hist_range=(0, 256))

# Create an array stack of feature vectors
X = np.vstack((car_features, notcar_features)).astype(np.float64)
# Fit a per-column scaler
X_scaler = StandardScaler().fit(X)
# Apply the scaler to X
scaled_X = X_scaler.transform(X)

# Define the labels vector
y = np.hstack((np.ones(len(car_features)), np.zeros(len(notcar_features))))

# Split up data into randomized training and test sets
rand_state = np.random.randint(0, 100)
X_train, X_test, y_train, y_test = train_test_split(
    scaled_X, y, test_size=0.2, random_state=rand_state)

print('Using spatial binning of:', spatial,
      'and', histbin, 'histogram bins')
print('Feature vector length:', len(X_train[0]))
# Use a linear SVC
svc = LinearSVC()
# Check the training time for the SVC
t = time.time()
svc.fit(X_train, y_train)
t2 = time.time()
print(round(t2 - t, 2), 'Seconds to train SVC...')
# Check the score of the SVC
print('Test Accuracy of SVC = ', round(svc.score(X_test, y_test), 4))
# Check the prediction time for a single sample
t = time.time()
n_predict = 10
print('My SVC predicts: ', svc.predict(X_test[0:n_predict]))
print('For these', n_predict, 'labels: ', y_test[0:n_predict])
t2 = time.time()
print(round(t2 - t, 5), 'Seconds to predict', n_predict, 'labels with SVC')

#
# HOG Classify
#


# Reduce the sample size because HOG features are slow to compute
# The quiz evaluator times out after 13s of CPU time
sample_size = 500
cars = cars[0:sample_size]
notcars = notcars[0:sample_size]

# Tweak these parameters and see how the results change.
colorspace = 'RGB'  # Can be RGB, HSV, LUV, HLS, YUV, YCrCb
orient = 9
pix_per_cell = 8
cell_per_block = 2
hog_channel = 0  # Can be 0, 1, 2, or "ALL"

t = time.time()
car_features = CarDetection.extract_features_hog(cars, cspace=colorspace, orient=orient,
                                                 pix_per_cell=pix_per_cell, cell_per_block=cell_per_block,
                                                 hog_channel=hog_channel)
notcar_features = CarDetection.extract_features_hog(notcars, cspace=colorspace, orient=orient,
                                                    pix_per_cell=pix_per_cell, cell_per_block=cell_per_block,
                                                    hog_channel=hog_channel)
t2 = time.time()
print(round(t2 - t, 2), 'Seconds to extract HOG features...')
# Create an array stack of feature vectors
X = np.vstack((car_features, notcar_features)).astype(np.float64)
# Fit a per-column scaler
X_scaler = StandardScaler().fit(X)
# Apply the scaler to X
scaled_X = X_scaler.transform(X)

# Define the labels vector
y = np.hstack((np.ones(len(car_features)), np.zeros(len(notcar_features))))

# Split up data into randomized training and test sets
rand_state = np.random.randint(0, 100)
X_train, X_test, y_train, y_test = train_test_split(
    scaled_X, y, test_size=0.2, random_state=rand_state)

print('Using:', orient, 'orientations', pix_per_cell,
      'pixels per cell and', cell_per_block, 'cells per block')
print('Feature vector length:', len(X_train[0]))
# Use a linear SVC
svc = LinearSVC()
# Check the training time for the SVC
t = time.time()
svc.fit(X_train, y_train)
t2 = time.time()
print(round(t2 - t, 2), 'Seconds to train SVC...')
# Check the score of the SVC
print('Test Accuracy of SVC = ', round(svc.score(X_test, y_test), 4))
# Check the prediction time for a single sample
t = time.time()
n_predict = 10
print('My SVC predicts: ', svc.predict(X_test[0:n_predict]))
print('For these', n_predict, 'labels: ', y_test[0:n_predict])
t2 = time.time()
print(round(t2 - t, 5), 'Seconds to predict', n_predict, 'labels with SVC')

#
# Sliding Window Implementation
#


image = mpimg.imread('input_images/bbox-example-image.jpg')
windows = CarDetection.slide_window(image, x_start_stop=[None, None], y_start_stop=[None, None],
                                    xy_window=(128, 128), xy_overlap=(0.5, 0.5))

window_img = CarDetection.draw_boxes(image, windows, color=(0, 0, 255), thick=6)
plt.imshow(window_img)
plt.savefig('output_images/test-slide_window.jpg')
plt.close()
