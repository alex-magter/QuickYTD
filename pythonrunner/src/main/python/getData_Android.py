from pytubefix import YouTube
import requests
from collections import OrderedDict
import sys
import csv
import os

# Create the folder that will contain the output files
def prepareOutput(context_path):  # Receive the context path from Kotlin
    # Use the context path to create a directory in the app's internal storage
    output_path = os.path.join(context_path, 'dataOutput')
    os.makedirs(output_path, exist_ok=True)  # Create the directory if it doesn't exist
    return os.path.abspath(output_path)

# Simply convert the given value in bytes to MiB
def byteToMb(value):
    result = round(value/1048576, 2)
    return str(result) + "MiB"

# Converts the video data given in list of lists to a csv file
def listsToCsv(lists, outputPath):
    with open(os.path.join(outputPath, "output.csv"), mode='w', newline='') as file:
        writer = csv.writer(file, delimiter=';')

        writer.writerows(lists)

# Downloads the thumbnail of the video
def downloadThumbnail(url, outputPath):
    yt = YouTube(url)
    video_id = yt.video_id
    thumbnail_url = f'https://img.youtube.com/vi/{video_id}/maxresdefault.jpg' # This is the path to yt thumbnails

    response = requests.get(thumbnail_url) # Download the thumbnail from the previous link

    if response.status_code == 200: # If request is successful
        with open(os.path.join(outputPath, 'img.jpg'), 'wb') as file: # Save the thumbain in a jpg file
            file.write(response.content)

# Save the name of the video and
# the name of the channel to a txt file
def getNameandChannel(url, outputPath):
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
def getData(url, outputPath):
    yt = YouTube(url)
    yt_streams = yt.streams

    # Filters the videos and audios separately with the
    # desired values.
    # In the real app video will always have the highest audio
    # quality so no worries to specify that in the program output
    audios = yt_streams.filter(only_audio=True, file_extension="mp4").order_by("abr").desc()
    videos = yt_streams.filter(only_video=True)

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
        newRow.append(byteToMb(webm.filesize))
        if "vp9" in webm.video_codec:
            output.append(newRow)

    mp4Videos = videos
    mp4Videos.filter(file_extension='mp4')
    mp4Videos.order_by("resolution").desc()

    # Now we filter for mp4 videos
    for mp4 in mp4Videos:
        newRow = list()
        newRow.append('Video')
        newRow.append('mp4')
        newRow.append(mp4.resolution)
        newRow.append(byteToMb(mp4.filesize))

        if "avc1." in mp4.video_codec: # For 1080p and below we use h264 for compatibility
            output.append(newRow)
        elif mp4.resolution == "1440p" or mp4.resolution == "2160p": # but in above resolution there is no h264 in pytube...
            output.append(newRow)                                    # ... so we download the available one


    listsToCsv(output, outputPath) # And we convert the data to csv to use it in kotlin

def startScript(video_url, context_path):
    outputPath = prepareOutput(context_path)
    getData(video_url, outputPath)
    downloadThumbnail(video_url, outputPath)
    getNameandChannel(video_url, outputPath)
    return outputPath