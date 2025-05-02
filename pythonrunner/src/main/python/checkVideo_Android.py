import sys

from pytubefix import YouTube

def checkVideo(url):
    try:
        yt = YouTube(url)
        print("valid")
        return True
    except Exception as e:
        print("invalid")
        return False