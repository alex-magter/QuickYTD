from pytubefix import YouTube
import requests
from collections import OrderedDict
import sys
import csv
import os

# Create the folder that will contain the output files
def prepareOutput():
    temp_path = os.environ.get('TEMP')
    os.chdir(temp_path)

    os.path.join(temp_path, 'dataOutput')
    if not os.path.exists('dataOutput'):
        os.makedirs('dataOutput')

    return os.path.abspath('dataOutput')

# Simply convert the given value in bytes to MiB
def byteToMb(value):
    result = round(value/1048576, 2)
    return str(result) + "MiB"

# Converts the video data given in list of lists to a csv file
def listsToCsv(lists):
    with open(os.path.join(outputPath, "output.csv"), mode='w', newline='') as file:
        writer = csv.writer(file, delimiter=';')

        writer.writerows(lists)

# Downloads the thumbnail of the video
def downloadThumbnail(url):
    yt = YouTube(url)
    video_id = yt.video_id
    thumbnail_url = f'https://img.youtube.com/vi/{video_id}/maxresdefault.jpg' # This is the path to yt thumbnails

    response = requests.get(thumbnail_url) # Download the thumbnail from the previous link

    if response.status_code == 200: # If request is successful
        with open(os.path.join(outputPath, 'img.jpg'), 'wb') as file: # Save the thumbain in a jpg file
            file.write(response.content)

# Save the name of the video and
# the name of the channel to a txt file
def getNameandChannel(url):
    # Get the names with the PyTube API...
    yt = YouTube(url)
    name = yt.title
    channel = yt.author

    # ... and save them to a txt file
    with open(os.path.join(outputPath, 'name.txt'), 'w', encoding='utf-8') as file:
        file.write(name)

    with open(os.path.join(outputPath,'channel.txt'), 'w', encoding='utf-8') as file:
        file.write(channel)



# Gets the posible downloads of the video
# and saves them to a list of lists to
# convert them to a csv table:
# Audio/Video | Extension | Resolution | Size
def getData(url):
    yt = YouTube(url)
    yt_streams = yt.streams

    # Filters the videos and audios separately with the
    # desired values.
    # In the real app video will always have the highest audio
    # quality so no worries to specify that in the program output
    audios = yt_streams.filter(only_audio=True, file_extension="mp4").order_by("abr").desc()
    videos = yt_streams.filter(only_video=True)

    highestAudioFilesize = yt_streams.filter(only_audio=True, file_extension="mp4").order_by("abr").desc().first().filesize

    # The list of lists that will contain the output
    output = list()

    # First, we get the values for each audio in mp4
    # PyTube always downloads in m4a
    for m4a in audios:
        newRow = list()
        newRow.append('Audio')
        newRow.append('m4a')
        newRow.append(m4a.abr)
        newRow.append(byteToMb(m4a.filesize))
        output.append(newRow)

    """

    webmVideos = videos
    webmVideos.filter(file_extension='webm')
    webmVideos.order_by("resolution").desc()

    # Now we get the values for WebM videos
    # It filters vp9 videos
    for webm in webmVideos:
        newRow = list()
        newRow.append('Video')
        newRow.append('webm')
        newRow.append(webm.resolution)
        newRow.append(byteToMb(webm.filesize + highestAudioFilesize))
        if "vp9" in webm.video_codec:
            output.append(newRow)
    """

    mp4Videos = videos
    mp4Videos.filter(file_extension='mp4')
    mp4Videos.order_by("resolution").desc()

    # Now we filter for mp4 videos
    for mp4 in mp4Videos:
        newRow = list()
        newRow.append('Video')
        newRow.append('mp4')
        newRow.append(mp4.resolution)
        newRow.append(byteToMb(mp4.filesize + highestAudioFilesize))

        if "avc1." in mp4.video_codec: # For 1080p and below we use h264 for compatibility
            output.append(newRow)
        elif mp4.resolution == "1440p" or mp4.resolution == "2160p": # but in above resolution there is no h264 in pytube...
            output.append(newRow)

    """

    # Now we get the values for WebM videos
    # It filters vp9 videos
    for webm in webmVideos:
        newRow = list()
        newRow.append('Video (muted)')
        newRow.append('webm')
        newRow.append(webm.resolution)
        newRow.append(byteToMb(webm.filesize))
        if "vp9" in webm.video_codec:
            output.append(newRow)
            
    """

    mp4Videos = videos
    mp4Videos.filter(file_extension='mp4')
    mp4Videos.order_by("resolution").desc()

    # Now we filter for mp4 videos
    for mp4 in mp4Videos:
        newRow = list()
        newRow.append('Video (muted)')
        newRow.append('mp4')
        newRow.append(mp4.resolution)
        newRow.append(byteToMb(mp4.filesize))

        if "avc1." in mp4.video_codec: # For 1080p and below we use h264 for compatibility
            output.append(newRow)
        elif mp4.resolution == "1440p" or mp4.resolution == "2160p": # but in above resolution there is no h264 in pytube...
            output.append(newRow)                                    # ... so we download the available one

    listsToCsv(output) # And we convert the data to csv to use it in kotlin


# Gets the Url and calls the functions
if __name__ == "__main__":
    video_url = sys.argv[1]
    outputPath = prepareOutput()
    getData(video_url)
    downloadThumbnail(video_url)
    getNameandChannel(video_url)
    print(outputPath)
