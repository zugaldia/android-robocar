"""

Identify the lane boundaries in a video from a front-facing camera on a car.

The steps of this process are the following:

- Compute the camera calibration matrix and distortion coefficients given a set of chessboard images.
- Apply a distortion correction to raw images.
- Use color transforms, gradients, etc., to create a thresholded binary image.
- Apply a perspective transform to rectify binary image ("birds-eye view").
- Detect lane pixels and fit to find the lane boundary.
- Determine the curvature of the lane and vehicle position with respect to center.
- Warp the detected lane boundaries back onto the original image.
- Output visual display of the lane boundaries and numerical estimation of lane curvature and vehicle position.

"""

from moviepy.editor import VideoFileClip
from libs.lane_detector import LaneDetector

#
# Prepare camera params
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

#
#
#


#
# Frame processing (core of the pipeline process)
#

lane_detector = LaneDetector()


def process_image(image):
    global lane_detector
    undistorted_image = LaneDetector.undistort(src=image, camera_matrix=camera_matrix, dist_coeffs=dist_coeffs)
    perspective = LaneDetector.perspective(undistorted_image)
    binary_warped = LaneDetector.combine_threshold_color(perspective)
    lane_detector.sliding_window_search(undistorted_image, binary_warped)
    return lane_detector.reprojected


#
# Process video
#

# TODO: get_curvature
print('Processing video.')
# clip1 = VideoFileClip('CarND-Advanced-Lane-Lines/project_video.mp4').subclip(0, 5)
clip1 = VideoFileClip('CarND-Advanced-Lane-Lines/project_video.mp4')
white_clip = clip1.fl_image(process_image)
white_clip.write_videofile('output/pipeline-project_video.mp4', audio=False)
