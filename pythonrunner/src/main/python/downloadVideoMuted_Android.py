import sys
import os

from pytubefix import YouTube
import re


def on_progress(stream, chunk, bytes_remaining):
    total_size = stream.filesize
    bytes_downloaded = total_size - bytes_remaining

    progress = (bytes_downloaded / total_size) * 100

    progress_callback(float(progress), "Downloading...")

    if (is_action_cancelled()):
        raise Exception("Download cancelled")


def download(url, ext, res, pathToDownload, filename):
    global finalTitle
    extension = ext
    yt = YouTube(url,
                 on_progress_callback=on_progress)
    streams = yt.streams.filter(only_video=True, file_extension=extension, resolution=res).order_by("resolution").desc()

    title = filename
    cleanTitle = re.sub(r'[<>:"/\\|?*\n¿]', '', title)

    uniqueFileNameFound = False
    attempts = 0

    name, extension = os.path.splitext(cleanTitle)

    while not uniqueFileNameFound:
        path = os.path.join(pathToDownload, cleanTitle)

        if os.path.exists(path):
            attempts += 1
            cleanTitle = f'{name}({attempts}){extension}'
        else:
            if attempts == 0:
                finalTitle = cleanTitle
                uniqueFileNameFound = True

            else:
                finalTitle = f'{name}({attempts}){extension}'
                uniqueFileNameFound = True

    print("Vamos a descargar...")

    try:

        streams.first().download(output_path=pathToDownload, filename=finalTitle)
        print(f"File downloaded in {pathToDownload} with name: {finalTitle}")

    except Exception as e:
        print(e)

