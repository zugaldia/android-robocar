from libs.convolution_search import ConvolutionSearch
from libs.lane_detector import LaneDetector
import cv2
import glob

#
# Distortion correction
#

CALIBRATION_FILE = 'output/calibration.pickle'

try:
    print('Loading previous calibration.')
    camera_matrix, dist_coeffs = LaneDetector.load_calibration(CALIBRATION_FILE)
except Exception as e:
    print('Generating calibration (%s).' % e)
    object_points, image_points, image_size = LaneDetector.get_calibration_params()
    camera_matrix, dist_coeffs = LaneDetector.calibrate_camera(object_points=object_points,
                                                               image_points=image_points,
                                                               image_size=image_size)
    LaneDetector.save_calibration(CALIBRATION_FILE, camera_matrix, dist_coeffs)

images = glob.glob('CarND-Advanced-Lane-Lines/camera_cal/calibration*.jpg')
for image in images:
    test_image = cv2.imread(image)
    undistorted = LaneDetector.undistort(src=test_image, camera_matrix=camera_matrix, dist_coeffs=dist_coeffs)
    cv2.imwrite('output/sandbox-undistorted-' + image[image.rfind('/') + 1:], undistorted)

#
# Test image
#

image = cv2.imread('CarND-Advanced-Lane-Lines/test_images/straight_lines1.jpg')
width, height = LaneDetector.get_img_size(image)
print(width, height)

#
# Gradient thresholding
#

LaneDetector.save_binary_output('output/sandbox-sobel_thresh_x.jpg',
                                LaneDetector.abs_sobel_thresh(image, orient='x', thresh=(20, 100)))
LaneDetector.save_binary_output('output/sandbox-sobel_thresh_y.jpg',
                                LaneDetector.abs_sobel_thresh(image, orient='y', thresh=(20, 100)))
LaneDetector.save_binary_output('output/sandbox-mag_thresh.jpg',
                                LaneDetector.mag_thresh(image, sobel_kernel=3, mag_thresh=(20, 100)))
LaneDetector.save_binary_output('output/sandbox-dir_threshold.jpg',
                                LaneDetector.dir_threshold(image, sobel_kernel=3, thresh=(0.7, 1.3)))
LaneDetector.save_binary_output('output/sandbox-combined_threshold.jpg',
                                LaneDetector.combined_threshold(image))

#
# Color thresholding
#

LaneDetector.save_binary_output('output/sandbox-hls_select.jpg', LaneDetector.hls_select(image, thresh=(90, 255)))

#
# Combine both
#

LaneDetector.save_binary_output('output/sandbox-combine_threshold_color.jpg', LaneDetector.combine_threshold_color(image))

#
# Warping
#

src = LaneDetector.get_warp_src()
warped = LaneDetector.warp(image, src, LaneDetector.get_warp_dst(src, height, width))
cv2.imwrite('output/sandbox-warped.jpg', warped)

#
# Pipeline
#

lane_detector = LaneDetector()
image = cv2.imread('CarND-Advanced-Lane-Lines/test_images/straight_lines1.jpg')

undistorted_image = LaneDetector.undistort(src=image, camera_matrix=camera_matrix, dist_coeffs=dist_coeffs)
cv2.imwrite('output/sandbox-undistorted_image.jpg', warped)

perspective = LaneDetector.perspective(undistorted_image)
cv2.imwrite('output/sandbox-perspective.jpg', warped)

binary_warped = LaneDetector.combine_threshold_color(perspective)
LaneDetector.save_binary_output('output/sandbox-binary_warped.jpg', warped)

lane_detector.sliding_window_search(undistorted_image, binary_warped)
if lane_detector.reprojected is not None:
    print('Lanes found')
    cv2.imwrite('output/sandbox-reprojected.jpg', lane_detector.reprojected)

# def get_curvature(ploty, leftx, rightx, y_eval):

#
# Convolution
#

search = ConvolutionSearch(binary_warped)
search.run()
