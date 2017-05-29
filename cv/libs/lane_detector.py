import glob
import cv2
import numpy as np
import matplotlib.pyplot as plt
import pickle

from libs.line import Line


class LaneDetector(object):
    def __init__(self):
        self.left_line = Line()
        self.right_line = Line()
        self.left_fit, self.right_fit = None, None
        self.reprojected = None

    #
    # Camera calibration
    #

    @staticmethod
    def load_calibration(calibration_file):
        with open(calibration_file, 'rb') as f:
            calibration = pickle.load(f)
        camera_matrix = calibration['camera_matrix']
        dist_coeffs = calibration['dist_coeffs']
        return camera_matrix, dist_coeffs

    @staticmethod
    def save_calibration(calibration_file, camera_matrix, dist_coeffs):
        with open(calibration_file, 'wb') as f:
            pickle.dump({'camera_matrix': camera_matrix, 'dist_coeffs': dist_coeffs}, f)

    @staticmethod
    def get_calibration_params():
        inside_corners_x = 9
        inside_corners_y = 6

        # Prepare object points, like (0,0,0), (1,0,0), (2,0,0) ....,(8,5,0)
        obj_point = np.zeros((inside_corners_y * inside_corners_x, 3), np.float32)
        obj_point[:, :2] = np.mgrid[0:inside_corners_x, 0:inside_corners_y].T.reshape(-1, 2)

        # Arrays to store object points and image points from all the images.
        object_points = []  # 3d points in real world space
        image_points = []  # 2d points in image plane
        image_size = None

        # Make a list of calibration images
        images = glob.glob('CarND-Advanced-Lane-Lines/camera_cal/calibration*.jpg')
        print('Calibration images found: %d' % len(images))

        # Step through the list and search for chessboard corners
        for image in images:
            print('Processing: %s' % image)
            image_color = cv2.imread(image)
            if image_size is None:
                image_size = LaneDetector.get_img_size(image_color)

            # Find the chessboard corners
            image_gray = cv2.cvtColor(image_color, cv2.COLOR_BGR2GRAY)
            ret, corners = cv2.findChessboardCorners(image_gray, (inside_corners_x, inside_corners_y), None)

            # If found, add object points, image points
            if ret:
                object_points.append(obj_point)
                image_points.append(corners)

                # Draw and display the corners
                image_corners = cv2.drawChessboardCorners(image_color, (inside_corners_x, inside_corners_y),
                                                          corners, ret)
                cv2.imwrite('output/corners-' + image[image.rfind('/') + 1:], image_corners)

        return object_points, image_points, image_size

    @staticmethod
    def calibrate_camera(object_points, image_points, image_size):
        retval, camera_matrix, dist_coeffs, rvecs, tvecs = cv2.calibrateCamera(objectPoints=object_points,
                                                                               imagePoints=image_points,
                                                                               imageSize=image_size, cameraMatrix=None,
                                                                               distCoeffs=None)
        return camera_matrix, dist_coeffs

    @staticmethod
    def undistort(src, camera_matrix, dist_coeffs):
        dst = cv2.undistort(src=src, cameraMatrix=camera_matrix, distCoeffs=dist_coeffs, dst=None,
                            newCameraMatrix=camera_matrix)
        return dst

    #
    # Thresholding
    #

    # Define a function that takes an image, gradient orientation, and threshold min / max values.
    @staticmethod
    def abs_sobel_thresh(img, orient='x', sobel_kernel=3, thresh=(0, 255)):
        # Convert to grayscale
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        # Apply x or y gradient with the OpenCV Sobel() function
        # and take the absolute value
        if orient == 'x':
            abs_sobel = np.absolute(cv2.Sobel(gray, cv2.CV_64F, 1, 0, ksize=sobel_kernel))
        else:  # y
            abs_sobel = np.absolute(cv2.Sobel(gray, cv2.CV_64F, 0, 1, ksize=sobel_kernel))
        # Rescale back to 8 bit integer
        scaled_sobel = np.uint8(255 * abs_sobel / np.max(abs_sobel))
        # Create a copy and apply the threshold
        binary_output = np.zeros_like(scaled_sobel)
        # Here I'm using inclusive (>=, <=) thresholds, but exclusive is ok too
        binary_output[(scaled_sobel >= thresh[0]) & (scaled_sobel <= thresh[1])] = 1

        # Return the result
        return binary_output

    # Define a function to return the magnitude of the gradient
    # for a given sobel kernel size and threshold values
    @staticmethod
    def mag_thresh(img, sobel_kernel=3, mag_thresh=(0, 255)):
        # Convert to grayscale
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        # Take both Sobel x and y gradients
        sobelx = cv2.Sobel(gray, cv2.CV_64F, 1, 0, ksize=sobel_kernel)
        sobely = cv2.Sobel(gray, cv2.CV_64F, 0, 1, ksize=sobel_kernel)
        # Calculate the gradient magnitude
        gradmag = np.sqrt(sobelx ** 2 + sobely ** 2)
        # Rescale to 8 bit
        scale_factor = np.max(gradmag) / 255
        gradmag = (gradmag / scale_factor).astype(np.uint8)
        # Create a binary image of ones where threshold is met, zeros otherwise
        binary_output = np.zeros_like(gradmag)
        binary_output[(gradmag >= mag_thresh[0]) & (gradmag <= mag_thresh[1])] = 1

        # Return the binary image
        return binary_output

    # Define a function to threshold an image for a given range and Sobel kernel
    @staticmethod
    def dir_threshold(img, sobel_kernel=3, thresh=(0, np.pi / 2)):
        # Grayscale
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        # Calculate the x and y gradients
        sobelx = cv2.Sobel(gray, cv2.CV_64F, 1, 0, ksize=sobel_kernel)
        sobely = cv2.Sobel(gray, cv2.CV_64F, 0, 1, ksize=sobel_kernel)
        # Take the absolute value of the gradient direction,
        # apply a threshold, and create a binary image result
        absgraddir = np.arctan2(np.absolute(sobely), np.absolute(sobelx))
        binary_output = np.zeros_like(absgraddir)
        binary_output[(absgraddir >= thresh[0]) & (absgraddir <= thresh[1])] = 1

        # Return the binary image
        return binary_output

    @staticmethod
    def combined_threshold(image):
        # Choose a Sobel kernel size
        ksize = 3  # Choose a larger odd number to smooth gradient measurements

        # Apply each of the thresholding functions
        gradx = LaneDetector.abs_sobel_thresh(image, orient='x', sobel_kernel=ksize, thresh=(20, 100))
        grady = LaneDetector.abs_sobel_thresh(image, orient='y', sobel_kernel=ksize, thresh=(20, 100))
        mag_binary = LaneDetector.mag_thresh(image, sobel_kernel=ksize, mag_thresh=(20, 100))
        dir_binary = LaneDetector.dir_threshold(image, sobel_kernel=ksize, thresh=(0.7, 1.3))
        binary_output = np.zeros_like(dir_binary)
        binary_output[((gradx == 1) & (grady == 1)) | ((mag_binary == 1) & (dir_binary == 1))] = 1
        return binary_output

    # Define a function that thresholds the S-channel of HLS
    @staticmethod
    def hls_select(img, thresh=(0, 255)):
        # Now you can see that, the S channel is still doing a fairly robust job of
        # picking up the lines under very different color and contrast conditions,
        # while the other selections look messy. You could tweak the thresholds and
        # get closer in the other channels, but the S channel is preferable because
        # it is more robust to changing conditions.
        #
        # It's worth noting, however, that the R channel still does rather well on
        # the white lines, perhaps even better than the S channel. As with gradients,
        # it's worth considering how you might combine various color thresholds to make
        # the most robust identification of the lines.
        hls = cv2.cvtColor(img, cv2.COLOR_BGR2HLS)
        s_channel = hls[:, :, 2]
        binary_output = np.zeros_like(s_channel)
        binary_output[(s_channel > thresh[0]) & (s_channel <= thresh[1])] = 1
        return binary_output

    @staticmethod
    def combine_threshold_color(image):
        combined = LaneDetector.combined_threshold(image)
        hls_binary = LaneDetector.hls_select(image, thresh=(90, 255))

        binary_output = np.zeros_like(combined)
        binary_output[(hls_binary == 1) | (combined == 1)] = 1
        return binary_output

    @staticmethod
    def warp(img, src, dst):
        # Compute and apply perspective transform
        img_size = LaneDetector.get_img_size(img=img)
        m = cv2.getPerspectiveTransform(src, dst)
        warped = cv2.warpPerspective(img, m, img_size, flags=cv2.INTER_NEAREST)  # keep same size as input image
        return warped

    @staticmethod
    def perspective(image):
        src = LaneDetector.get_warp_src()

        # Debug only
        # plt.imshow(image)
        # plt.plot(src[0][0], src[0][1], '.')  # top left (x, y)
        # plt.plot(src[1][0], src[1][1], '.')  # top right
        # plt.plot(src[2][0], src[2][1], '.')  # bottom left
        # plt.plot(src[3][0], src[3][1], '.')  # bottom right
        # plt.savefig('output/pipeline-undistorted_points.jpg')
        # plt.close()

        width, height = LaneDetector.get_img_size(image)
        dst = LaneDetector.get_warp_dst(src, height, width)
        warped = LaneDetector.warp(image, src, dst)
        return warped

    @staticmethod
    def get_warp_src():
        # We manually pick these
        src = np.float32([[594, 451],
                          [685, 451],
                          [268, 677],
                          [1037, 677]])
        return src

    @staticmethod
    def get_warp_dst(src, height, width):
        offset_x = 100
        offset_y = offset_x * height / width
        dst = np.float32([[src[2][0] + offset_x, offset_y],  # top left
                          [src[3][0] - offset_x, offset_y],  # top right
                          [src[2][0] + offset_x, height],  # bottom left
                          [src[3][0] - offset_x, height]])  # bottom right
        return dst

    @staticmethod
    def get_img_size(img):
        if img is None:
            raise Exception('Cannot get size of an empty image.')
        return img.shape[1], img.shape[0]

    @staticmethod
    def save_binary_output(filename, binary_output):
        plt.imshow(binary_output, cmap='gray')
        plt.savefig(filename)
        plt.close()

    def sliding_window_search(self, undistorted_image, binary_warped):
        try:
            if self.left_fit is not None and self.right_fit is not None:
                self.left_fit, self.right_fit, ploty, left_fitx, right_fitx, leftx, rightx = self.sliding_window_continue(
                    binary_warped, self.left_fit, self.right_fit)
            else:
                self.left_fit, self.right_fit, ploty, left_fitx, right_fitx, leftx, rightx = self.sliding_window_start(
                    undistorted_image, binary_warped)
            self.reprojected = self.reproject(undistorted_image, binary_warped, ploty, left_fitx, right_fitx)
        except Exception as e:
            print("Failed: %s" % e)
            self.reprojected = undistorted_image

        try:
            y_eval = np.max(ploty)
            left_curverad, right_curverad = self.get_curvature(ploty, left_fitx, right_fitx, y_eval)
            left_text = 'straight' if left_curverad > 2000 else '%.1fm' % left_curverad
            right_text = 'straight' if right_curverad > 2000 else '%.1fm' % right_curverad
            curvature_text = 'radius of curvature: %s (left), %s (right)' % (left_text, right_text)
            deviation = self.get_deviation(self.left_fit, self.right_fit)
            deviation_side = 'left' if deviation > 0 else 'right'
        except Exception as e:
            curvature_text = 'radius of curvature: %.1fm (left), %.1fm (right)' % (0.0, 0.0)
            deviation = 0.0
            deviation_side = ''
            print("Failed: %s" % e)

        cv2.putText(self.reprojected, curvature_text,
                    org=(50, 80), fontFace=cv2.FONT_HERSHEY_PLAIN, fontScale=1, color=(255, 255, 255))
        cv2.putText(self.reprojected, 'deviation from center: %.1fcm (to the %s)' % (abs(deviation) * 100, deviation_side),
                    org=(50, 100), fontFace=cv2.FONT_HERSHEY_PLAIN, fontScale=1, color=(255, 255, 255))

    @staticmethod
    def sliding_window_start(undistorted_image, binary_warped):
        # Assuming you have created a warped binary image called "binary_warped"
        # Take a histogram of the bottom half of the image
        histogram = np.sum(binary_warped[binary_warped.shape[0] // 2:, :], axis=0)

        # Debug only
        # plt.plot(histogram)
        # plt.savefig('output/pipeline-histogram.jpg')
        # plt.close()

        # Create an output image to draw on and  visualize the result
        out_img = np.dstack((binary_warped, binary_warped, binary_warped)) * 255
        # Find the peak of the left and right halves of the histogram
        # These will be the starting point for the left and right lines
        midpoint = np.int(histogram.shape[0] / 2)
        leftx_base = np.argmax(histogram[:midpoint])
        rightx_base = np.argmax(histogram[midpoint:]) + midpoint

        # Choose the number of sliding windows
        nwindows = 9
        # Set height of windows
        window_height = np.int(binary_warped.shape[0] / nwindows)
        # Identify the x and y positions of all nonzero pixels in the image
        nonzero = binary_warped.nonzero()
        nonzeroy = np.array(nonzero[0])
        nonzerox = np.array(nonzero[1])
        # Current positions to be updated for each window
        leftx_current = leftx_base
        rightx_current = rightx_base
        # Set the width of the windows +/- margin
        margin = 100
        # Set minimum number of pixels found to recenter window
        minpix = 50
        # Create empty lists to receive left and right lane pixel indices
        left_lane_inds = []
        right_lane_inds = []

        # Step through the windows one by one
        for window in range(nwindows):
            # Identify window boundaries in x and y (and right and left)
            win_y_low = binary_warped.shape[0] - (window + 1) * window_height
            win_y_high = binary_warped.shape[0] - window * window_height
            win_xleft_low = leftx_current - margin
            win_xleft_high = leftx_current + margin
            win_xright_low = rightx_current - margin
            win_xright_high = rightx_current + margin
            # Draw the windows on the visualization image
            cv2.rectangle(out_img, (win_xleft_low, win_y_low), (win_xleft_high, win_y_high), (0, 255, 0), 2)
            cv2.rectangle(out_img, (win_xright_low, win_y_low), (win_xright_high, win_y_high), (0, 255, 0), 2)
            # Identify the nonzero pixels in x and y within the window
            good_left_inds = ((nonzeroy >= win_y_low) & (nonzeroy < win_y_high) & (nonzerox >= win_xleft_low) & (
                nonzerox < win_xleft_high)).nonzero()[0]
            good_right_inds = ((nonzeroy >= win_y_low) & (nonzeroy < win_y_high) & (nonzerox >= win_xright_low) & (
                nonzerox < win_xright_high)).nonzero()[0]
            # Append these indices to the lists
            left_lane_inds.append(good_left_inds)
            right_lane_inds.append(good_right_inds)
            # If you found > minpix pixels, recenter next window on their mean position
            if len(good_left_inds) > minpix:
                leftx_current = np.int(np.mean(nonzerox[good_left_inds]))
            if len(good_right_inds) > minpix:
                rightx_current = np.int(np.mean(nonzerox[good_right_inds]))

        # Concatenate the arrays of indices
        left_lane_inds = np.concatenate(left_lane_inds)
        right_lane_inds = np.concatenate(right_lane_inds)

        # Extract left and right line pixel positions
        leftx = nonzerox[left_lane_inds]
        lefty = nonzeroy[left_lane_inds]
        rightx = nonzerox[right_lane_inds]
        righty = nonzeroy[right_lane_inds]

        # Fit a second order polynomial to each
        left_fit = np.polyfit(lefty, leftx, 2)
        right_fit = np.polyfit(righty, rightx, 2)

        # Get viz params
        ploty, left_fitx, right_fitx = LaneDetector.visualize_window(undistorted_image, binary_warped, out_img,
                                                                     nonzerox, nonzeroy, left_lane_inds,
                                                                     right_lane_inds, left_fit, right_fit)
        return left_fit, right_fit, ploty, left_fitx, right_fitx, leftx, rightx

    @staticmethod
    def visualize_window(undistorted_image, binary_warped, out_img, nonzerox, nonzeroy, left_lane_inds, right_lane_inds,
                         left_fit, right_fit):
        # Generate x and y values for plotting
        ploty = np.linspace(0, binary_warped.shape[0] - 1, binary_warped.shape[0])
        left_fitx = left_fit[0] * ploty ** 2 + left_fit[1] * ploty + left_fit[2]
        right_fitx = right_fit[0] * ploty ** 2 + right_fit[1] * ploty + right_fit[2]

        # Reproject
        # LaneDetector.reproject(undistorted_image, binary_warped, ploty, left_fitx, right_fitx)
        # LaneDetector.get_curvature(ploty, left_fitx, right_fitx, y_eval=720)

        # out_img[nonzeroy[left_lane_inds], nonzerox[left_lane_inds]] = [255, 0, 0]
        # out_img[nonzeroy[right_lane_inds], nonzerox[right_lane_inds]] = [0, 0, 255]
        # plt.imshow(out_img)
        # plt.plot(left_fitx, ploty, color='yellow')
        # plt.plot(right_fitx, ploty, color='yellow')
        # plt.xlim(0, 1280)
        # plt.ylim(720, 0)
        # plt.savefig('output/pipeline-poly.jpg')
        # plt.close()

        return ploty, left_fitx, right_fitx

    @staticmethod
    def sliding_window_continue(binary_warped, left_fit, right_fit):
        # Assume you now have a new warped binary image
        # from the next frame of video (also called "binary_warped")
        # It's now much easier to find line pixels!
        nonzero = binary_warped.nonzero()
        nonzeroy = np.array(nonzero[0])
        nonzerox = np.array(nonzero[1])
        margin = 100
        left_lane_inds = (
            (nonzerox > (left_fit[0] * (nonzeroy ** 2) + left_fit[1] * nonzeroy + left_fit[2] - margin)) & (
                nonzerox < (left_fit[0] * (nonzeroy ** 2) + left_fit[1] * nonzeroy + left_fit[2] + margin)))
        right_lane_inds = (
            (nonzerox > (right_fit[0] * (nonzeroy ** 2) + right_fit[1] * nonzeroy + right_fit[2] - margin)) & (
                nonzerox < (right_fit[0] * (nonzeroy ** 2) + right_fit[1] * nonzeroy + right_fit[2] + margin)))

        # Again, extract left and right line pixel positions
        leftx = nonzerox[left_lane_inds]
        lefty = nonzeroy[left_lane_inds]
        rightx = nonzerox[right_lane_inds]
        righty = nonzeroy[right_lane_inds]
        # Fit a second order polynomial to each
        left_fit = np.polyfit(lefty, leftx, 2)
        right_fit = np.polyfit(righty, rightx, 2)

        # Get viz params
        ploty, left_fitx, right_fitx = LaneDetector.visualize_window_continue(binary_warped, nonzerox, nonzeroy,
                                                                              left_fit, right_fit, left_lane_inds,
                                                                              right_lane_inds, margin)

        return left_fit, right_fit, ploty, left_fitx, right_fitx, leftx, rightx

    @staticmethod
    def visualize_window_continue(binary_warped, nonzerox, nonzeroy, left_fit, right_fit, left_lane_inds,
                                  right_lane_inds, margin):
        # Generate x and y values for plotting
        ploty = np.linspace(0, binary_warped.shape[0] - 1, binary_warped.shape[0])
        left_fitx = left_fit[0] * ploty ** 2 + left_fit[1] * ploty + left_fit[2]
        right_fitx = right_fit[0] * ploty ** 2 + right_fit[1] * ploty + right_fit[2]

        # Create an image to draw on and an image to show the selection window
        out_img = np.dstack((binary_warped, binary_warped, binary_warped)) * 255
        window_img = np.zeros_like(out_img)
        # Color in left and right line pixels
        out_img[nonzeroy[left_lane_inds], nonzerox[left_lane_inds]] = [255, 0, 0]
        out_img[nonzeroy[right_lane_inds], nonzerox[right_lane_inds]] = [0, 0, 255]

        # Generate a polygon to illustrate the search window area
        # And recast the x and y points into usable format for cv2.fillPoly()
        left_line_window1 = np.array([np.transpose(np.vstack([left_fitx - margin, ploty]))])
        left_line_window2 = np.array([np.flipud(np.transpose(np.vstack([left_fitx + margin, ploty])))])
        left_line_pts = np.hstack((left_line_window1, left_line_window2))
        right_line_window1 = np.array([np.transpose(np.vstack([right_fitx - margin, ploty]))])
        right_line_window2 = np.array([np.flipud(np.transpose(np.vstack([right_fitx + margin, ploty])))])
        right_line_pts = np.hstack((right_line_window1, right_line_window2))

        # Draw the lane onto the warped blank image
        cv2.fillPoly(window_img, np.int_([left_line_pts]), (0, 255, 0))
        cv2.fillPoly(window_img, np.int_([right_line_pts]), (0, 255, 0))
        result = cv2.addWeighted(out_img, 1, window_img, 0.3, 0)
        # plt.imshow(result)
        # plt.plot(left_fitx, ploty, color='yellow')
        # plt.plot(right_fitx, ploty, color='yellow')
        # plt.xlim(0, 1280)
        # plt.ylim(720, 0)
        # plt.savefig('output/pipeline-poly_continue.jpg')
        # plt.close()

        return ploty, left_fitx, right_fitx

    @staticmethod
    def get_curvature(ploty, leftx, rightx, y_eval):
        # Define conversions in x and y from pixels space to meters
        ym_per_pix = 30 / 720  # meters per pixel in y dimension
        xm_per_pix = 3.7 / 700  # meters per pixel in x dimension

        # Fit new polynomials to x,y in world space
        left_fit_cr = np.polyfit(ploty * ym_per_pix, leftx * xm_per_pix, 2)
        right_fit_cr = np.polyfit(ploty * ym_per_pix, rightx * xm_per_pix, 2)
        # Calculate the new radii of curvature
        left_curverad = ((1 + (2 * left_fit_cr[0] * y_eval * ym_per_pix + left_fit_cr[1]) ** 2) ** 1.5) / np.absolute(
            2 * left_fit_cr[0])
        right_curverad = (
                             (1 + (
                                 2 * right_fit_cr[0] * y_eval * ym_per_pix + right_fit_cr[
                                     1]) ** 2) ** 1.5) / np.absolute(
            2 * right_fit_cr[0])
        # Now our radius of curvature is in meters
        # Example values: 632.1 m    626.2 m
        return left_curverad, right_curverad

    @staticmethod
    def get_deviation(left_fit, right_fit):
        # Input images are 1280x720
        xm_per_pix = 3.7 / 700  # meters per pixel in x dimension
        leftx_int = left_fit[0] * 720 ** 2 + left_fit[1] * 720 + left_fit[2]
        rightx_int = right_fit[0] * 720 ** 2 + right_fit[1] * 720 + right_fit[2]
        deviation = (640 - ((rightx_int + leftx_int) / 2)) * xm_per_pix
        return deviation + (10/100)

    @staticmethod
    def reproject(undist, warped, ploty, left_fitx, right_fitx):
        # Create an image to draw the lines on
        warp_zero = np.zeros_like(warped).astype(np.uint8)
        color_warp = np.dstack((warp_zero, warp_zero, warp_zero))

        # Recast the x and y points into usable format for cv2.fillPoly()
        pts_left = np.array([np.transpose(np.vstack([left_fitx, ploty]))])
        pts_right = np.array([np.flipud(np.transpose(np.vstack([right_fitx, ploty])))])
        pts = np.hstack((pts_left, pts_right))

        # Draw the lane onto the warped blank image
        cv2.fillPoly(color_warp, np.int_([pts]), (0, 255, 0))

        # Warp the blank back to original image space using inverse perspective matrix (Minv)
        src = LaneDetector.get_warp_src()
        width, height = LaneDetector.get_img_size(undist)
        dst = LaneDetector.get_warp_dst(src, height, width)
        newwarp = LaneDetector.warp(color_warp, dst, src)

        # Combine the result with the original image
        result = cv2.addWeighted(undist, 1, newwarp, 0.3, 0)
        # plt.imshow(result)
        # plt.savefig('output/pipeline-reproject.jpg')
        # plt.close()
        return result
