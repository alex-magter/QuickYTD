import sys

from pytubefix import YouTube


def on_progress(stream, chunk, bytes_remaining):
    total_size = stream.filesize
    bytes_downloaded = total_size - bytes_remaining

    progress = (bytes_downloaded / total_size) * 100
    print(f"Progreso: {progress:.2f}%", flush=True)

def downloadAudio(url, ext, res):
    extension = "mp4" if ext == 'm4a' else 'webm'
    yt = YouTube(url,
    on_progress_callback=on_progress)
    streams = yt.streams.filter(only_audio=True, file_extension=extension, abr=res).order_by("abr").desc()

    print(streams.first())
    print(streams)
    streams.first().download()

def downloadVideo(url, ext, res):
    yt = YouTube(url)


if __name__ == "__main__":
    video_url = sys.argv[1]
    content_type = sys.argv[2]
    extension = sys.argv[3]
    resolution = sys.argv[4]

    print(f"content {content_type}")
    print(f"content {extension}")
    print(f"content {resolution}")

    if(content_type == 'video'):
        downloadVideo(video_url, extension, resolution)

    else:
        downloadAudio(video_url, extension, resolution)

