# Check PNG

from moviepy.editor import VideoFileClip
from scipy.ndimage.measurements import label

from libs.lesson_functions import *

try:
    dist_pickle = pickle.load(open("svc_pickle.p", "rb"))
    print('Training data found.')
except:
    print('Training data not found, training.')
    dist_pickle = do_training()


last_bboxes = []


def process_image(image):
    global dist_pickle, last_bboxes

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

    heat = np.zeros_like(image[:, :, 0]).astype(np.float)
    last_bboxes.append(bboxes)
    last_bboxes = last_bboxes[-6:]
    heat = add_heat(heat, [bbox for bboxes in last_bboxes for bbox in bboxes])
    heat = apply_threshold(heat, 4)
    heatmap = np.clip(heat, 0, 255)
    labels = label(heatmap)
    draw_img = draw_labeled_bboxes(np.copy(image), labels)
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
