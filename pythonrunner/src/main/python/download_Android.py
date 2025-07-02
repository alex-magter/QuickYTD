import sys

from pytubefix import YouTube
import re


def on_progress(stream, chunk, bytes_remaining):

    total_size = stream.filesize
    bytes_downloaded = total_size - bytes_remaining

    progress = (bytes_downloaded / total_size) * 100
    print(progress/100, flush=True)


def downloadAudio(url, ext, res, pathToDownload, filename, callbackOnProgress):

    extension = "mp4" if ext == 'm4a' else 'webm'
    yt = YouTube(url,
    on_progress_callback=on_progress)
    streams = yt.streams.filter(only_audio=True, file_extension=extension, abr=res).order_by("abr").desc()

    title = filename
    cleanTitle = re.sub(r'[<>:"/\\|?*\n¿]', '', title)

    print("Vamos a descargar...")

    try:

        streams.first().download(output_path=pathToDownload, filename=cleanTitle)
        print(f"File downloaded in {pathToDownload} with name: {cleanTitle}")

    except Exception as e:
        print(e)

def downloadVideo(url, ext, res):
    yt = YouTube(url)

