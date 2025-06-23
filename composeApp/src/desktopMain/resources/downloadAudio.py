import sys
import re

from pytubefix import YouTube


def on_progress(stream, chunk, bytes_remaining):
    total_size = stream.filesize
    bytes_downloaded = total_size - bytes_remaining

    progress = (bytes_downloaded / total_size) * 100
    print(progress, end='\n', flush=True)

def downloadAudio(url, ext, res, pathToDownload):

    extension = "mp4" if ext == 'm4a' else 'webm'
    yt = YouTube(url,
    on_progress_callback=on_progress)
    streams = yt.streams.filter(only_audio=True, file_extension=extension, abr=res).order_by("abr").desc()

    title = yt.title
    cleanTitle = re.sub(r'[<>:"/\\|?*\n¿]', '', title)

    try:
        streams.first().download(output_path=pathToDownload, filename=f"{cleanTitle}.m4a")

    except Exception as e:
        print(e)


if __name__ == "__main__":
    video_url = sys.argv[1]
    extension = sys.argv[2]
    resolution = sys.argv[3]
    folderToSave = sys.argv[4]

    downloadAudio(video_url, extension, resolution, folderToSave)

