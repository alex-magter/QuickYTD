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

    progress_callback(float(progress), downloadTag)

    if (is_action_cancelled()):
        raise Exception("Download cancelled")


def download(url, ext, res, pathToDownload, filenameAudio, filenameVideo):

    global finalTitle
    extension = ext
    downloadTag = ""

    yt = YouTube(url,
                 on_progress_callback=on_progress)
    streams = yt.streams.filter(only_audio=True, file_extension="mp4").order_by("abr").desc()


    print("Vamos a descargar el audio...")

    downloadTag = "Downloading audio..."

    try:

        streams.first().download(output_path=pathToDownload, filename=filenameAudio)
        print(f"Audio downloaded in {pathToDownload} with name: {filenameAudio}")

    except Exception as e:
        print(e)

    print("Audio descargado")



    yt = YouTube(url,
                 on_progress_callback=on_progress)
    streams = yt.streams.filter(only_video=True, file_extension=ext, resolution=res).order_by("resolution").desc()

    print("Vamos a descargar el Video...")

    downloadTag = "Downloading video..."

    try:

        streams.first().download(output_path=pathToDownload, filename=filenameVideo)
        print(f"Audio downloaded in {pathToDownload} with name: {filenameVideo}")

    except Exception as e:
        print(e)

    print("Video descargado")
