import sys
import os

from pytubefix import YouTube
import re
import tempfile
import subprocess

global downloadTag

def on_progress(stream, chunk, bytes_remaining):
    total_size = stream.filesize
    bytes_downloaded = total_size - bytes_remaining

    progress = (bytes_downloaded / total_size) * 100

    print(progress)


def downloadVideo(url, ext, res, pathToDownload, filenameAudio, filenameVideo):

    global finalTitle
    extension = ext
    downloadTag = ""

    yt = YouTube(url,
                 on_progress_callback=on_progress)
    streams = yt.streams.filter(only_audio=True, file_extension="mp4").order_by("abr").desc()



    downloadTag = "Downloading audio..."

    try:

        streams.first().download(output_path=pathToDownload, filename=filenameAudio)

    except Exception as e:
        print(e)




    yt = YouTube(url,
                 on_progress_callback=on_progress)
    streams = yt.streams.filter(only_video=True, file_extension=ext, resolution=res).order_by("resolution").desc()


    downloadTag = "Downloading video..."

    try:

        streams.first().download(output_path=pathToDownload, filename=filenameVideo)

    except Exception as e:
        print(e)


if __name__ == "__main__":
    video_url = sys.argv[1]
    extension = sys.argv[2]
    resolution = sys.argv[3]
    tempFolder = sys.argv[4]
    audioFilename = sys.argv[5]
    videoFilename = sys.argv[6]

    downloadVideo(video_url, extension, resolution, tempFolder, audioFilename, videoFilename)



