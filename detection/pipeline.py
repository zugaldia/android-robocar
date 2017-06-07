# Check PNG

import matplotlib.pyplot as plt
from scipy.ndimage.measurements import label
from moviepy.editor import VideoFileClip

from libs.lesson_functions import *

try:
    dist_pickle = pickle.load(open("svc_pickle.p", "rb"))
    print('Training data found.')
except:
    print('Training data not found, training.')
    dist_pickle = do_training()

bboxes_before = None


def process_image(image):
    global dist_pickle, bboxes_before

    # image = mpimg.imread('input_images/bbox-example-image.jpg')

    ystart = 400
    ystop = 656
    scale = 1.5
    svc = dist_pickle["svc"]
    X_scaler = dist_pickle["scaler"]
    orient = dist_pickle["orient"]
    pix_per_cell = dist_pickle["pix_per_cell"]
    cell_per_block = dist_pickle["cell_per_block"]
    spatial_size = dist_pickle["spatial_size"]
    hist_bins = dist_pickle["hist_bins"]

    bboxes = find_cars(image, ystart, ystop, scale, svc, X_scaler, orient,
                       pix_per_cell, cell_per_block, spatial_size, hist_bins)

    if len(bboxes) > 0:
        bboxes_before = bboxes
    elif bboxes_before is not None:
        bboxes = bboxes_before

    # Add heat to each box in box list
    heat = np.zeros_like(image[:, :, 0]).astype(np.float)
    heat = add_heat(heat, bboxes)

    # Apply threshold to help remove false positives
    heat = apply_threshold(heat, 1)

    # Visualize the heatmap when displaying
    heatmap = np.clip(heat, 0, 255)

    # Find final boxes from heatmap using label function
    labels = label(heatmap)
    draw_img = draw_labeled_bboxes(image, labels)

    # fig = plt.figure()
    # plt.subplot(121)
    # plt.imshow(draw_img)
    # plt.title('Car Positions')
    # plt.subplot(122)
    # plt.imshow(heatmap, cmap='hot')
    # plt.title('Heat Map')
    # fig.tight_layout()
    # plt.savefig('output_images/test-heat.jpg')
    # plt.close()

    return draw_img


# print('Processing video.')
# clip1 = VideoFileClip('CarND-Vehicle-Detection/test_video.mp4')
# white_clip = clip1.fl_image(process_image)
# white_clip.write_videofile('pipeline-test_video.mp4', audio=False)

print('Processing video.')
# clip1 = VideoFileClip('CarND-Vehicle-Detection/project_video.mp4').subclip(5, 15)
clip1 = VideoFileClip('CarND-Vehicle-Detection/project_video.mp4')
white_clip = clip1.fl_image(process_image)
white_clip.write_videofile('pipeline-project_video.mp4', audio=False)
