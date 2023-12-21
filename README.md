# ryokushu

Mod that draws thing.

## Downloads

See [releases](https://github.com/nonoteal/ryokushu/releases).

## Demo

To use: 
1) Open drawme folder using [`drawme open`](#drawme-open).
2) Place images and videos inside
3) To play...
    - Images: Know what image you want to place and refer to [`draw`](#draw-image-small-displaydevice-resizex-resizey-interval-nr-ng-nb-fill).
      - Placing images on servers can be aborted using [`drawme abort`](#drawme-stop). DO NOT USE [`drawme stop`](#drawme-stop), it is for aborting videos.
    - Videos: Find the video, [`drawme process`](#drawme-process-video-folder-width-fps) it if necessary, then [`play`](#play-image-small-displaydevice-mute-resizex-resizey-fps-nr-ng-nb-fill) it.
      - Pause the video using [`drawme pause`](#drawme-pause).
      - Stop the video using [`drawme stop`](#drawme-stop).
4) Get help with `drawme help`.

### See the video demo

[![drawing and videos in minecraft](https://img.youtube.com/vi/n66sPliVYas/maxres1.jpg)](https://www.youtube.com/watch?v=n66sPliVYas)

### Some Images

[![Chromium](https://i.ibb.co/YkKxnCP/2023-12-20-17-18-12.png)]()
[![VLC](https://i.ibb.co/fQXvD0G/2023-12-20-17-18-31.png)]()


## Commands

### `draw <image> <small> [<display_device>] [<resize_x>] [<resize_y>] [<interval>] [<nr>] [<ng>] [<nb>] [<fill>]`

Draw an image using armorstands.

- `<image>` is the path to your image.
- `<small>` will use periods instead of boxes to scale down the image in Minecraft at the same resolution.
- `[<display_device>]` indicates whether to use the new text_display entity instead of armorstands.
- `[<resize_x>]` resizes the width of the image. You may see 3 numbers: The original width, a number smaller than 480 (max height is 480), or 480 (max width is 480).
- `[<resize_y>]` resizes the height of the image. Leaving this blank will scale the height down appropriately.
- `[<interval>]` indicates how frequently to place an armorstand. Not needed for singleplayer, default is 75 (ms) for multiplayer. The higher this value is, the more likely armorstands will be placed.
- `[<nr>]` indicates whether to toggle the RED channel
- `[<ng>]` indicates whether to toggle the GREEN channel
- `[<nb>]` indicates whether to toggle the BLUE channel
- `[<fill>]` indicates what character to fill the names with. Overrides <small>.

### `play <image> <small> [<display_device>] [<mute>] [<resize_x>] [<resize_y>] [<fps>] [<nr>] [<ng>] [<nb>] [<fill>]`

Playback video using armorstands. The command parameters are similar to draw, the only difference being...

- `<image>` is the name of the folder with images inside. See the manual on how to set it up.
- `<mute>` will determine whether or not the attached video audio should be played. Audio will play by default, and volume is independent of Minecraft's sound system.
- `[<fps>]` is the frame rate to play the image sequence at. The default frame rate is 30 FPS.

### `drawme abort`
Stops drawing an image.

### `drawme pause`
Pauses a video.

### `drawme stop`
Stops a video from playing.

### `drawme open`
Simple text to open the drawme folder.

### `drawme process <video> <folder> [<width>] [<fps>]`
Processes a video in the drawme folder to then be played. FFmpeg is required.

- `<video>` is the name of the video in your drawme folder.
- `<folder>` is the name of the folder AND audio file to dump video and audio to.
- `[<width>]` is the new width of the image sequence.
- `[<fps>]` is the frame rate to write the image sequence as.

## Notes
### Creating images
This mod does not make any network requests to receive images. All files must be local and under the drawme folder in your minecraft folder.

### Video folders
Video folders are a sequence of images. Use ffmpeg on a video to extract the frames into a folder, OR use the `drawme process` command. Your command will look something like this: 

```
ffmpeg -i <video> -r <framerate> -vf scale=<desired image length>:-1 -compression_level 100 -q:v 5 ./folder/%06d.jpg
```

This command exports frames in a compressed format that do not take up too much disk space. The files are sorted using `Arrays.sort()`, so format your filenames as numbers. Please play videos at a width/length LESS than 480, maybe even 240, due to lag and how tall the video can get at larger lengths.

#### Audio file
When you allow audio on the [`play`](#play-image-small-displaydevice-mute-resizex-resizey-fps-nr-ng-nb-fill) command, it will play an audio file with the same name of the folder that contains the image sequence. For example, if your folder is located at `./folder/music/` where music is a folder containing an image sequence, then the audio file should be located at `./folder/music.wav`. The wav extension should be lowercase, and the filename is case-sensitive. Extract audio from your video using the following ffmpeg command: 

```
ffmpeg -i <video> -qscale 0 -vn ./folder/<name>.wav
``` 

This command exports the audio at the same quality while removing the video codec. WAV is an uncompressed lossless format, so the file size will be larger. Audio is played using javax libraries rather than Minecraft's internal sound system, so moving away from the video will not affect the volume of the audio.

### Servers
Draw is usable on servers, while play is clientside. If implemented serverside, play requires serverside access to manipulate the armorstands' names. Since Minecraft is not intended to play videos, the end result is astoundingly laggy even on a server running on localhost. An interval is recommended for servers when using draw, otherwise there will be missing lines of the image. This mod cannot remove [`draw`](#draw-image-small-displaydevice-resizex-resizey-interval-nr-ng-nb-fill) armorstands. [`play`](#play-image-small-displaydevice-mute-resizex-resizey-fps-nr-ng-nb-fill) armorstands' names are removed after playback is complete or `stop` is called.

### Multitasking
It is theoretically possible to play multiple videos at once, but this mod cannot do that. Playing one video is already putting a lot of stress on your client and disk drive. Additionally, drawing an image spams a spawn egg which makes drawing multiple images at once impossible given the current code.

### Processing Folders
Videos are processed by processing each frame when called rather than preprocessing an entire folder. Preprocessing may allow faster playback but ultimately sacrifices memory. When an individual frame is called, less memory is needed while sacrificing the CPU. Therefore, it is good to compress your images and to downscale them since you really would not want to see a 1920x1080 (1080p) video played 1:1 in Minecraft.

### Processing Videos
Since Java does not like me, you can't play a video directly in game. You first have to convert the video by using the [`drawme process`](#drawme-process-video-folder-width-fps) command. This command requires FFmpeg, and takes the same steps that the ReplayMod does to find it, so chances are if ReplayMod can render videos, then this command will work. This command automates how videos would be created based on [*Video Folders*](#video-folders).

### Text Display VS Armorstand
Text displays are only a feature for 1.20+. Text display rotations are static, while armorstands are dynamic. As a result, text displays are one-sided. The pitch of a text display can be modified but because of NBT limits, this mod does not take advantage of a text display's pitch.