# EMJPEGStreamer
Extended MJPEG live capture and streaming over websockets using Java and VLCJ

## Summary
This is a Java server and HTML/JS client. The server streams video data over websockets. Video is captured using VLCj (you can use any video input, but right now it's using screen:// input) and 
encoded using JPEG/PNG/GIF (JPEG gets a higher framerate). Bandwidth is kept low using a custom, simple interframe implementation.
On the client, the images are decoded by using an Image HTML element.

There are some neat tricks:
- Inter frame compression
  - The keyframe is subtracted from the interframe in compression, and added on decompression; thus minimizing scenes that are mostly static.
    - The algorithm is as followed: 127 + (keyframe(255) - interframe(255) / 2; Interframes are dominantly grey.
- Frames that are repeatant are not sent.
- There is interlacing.
- Adaptive JPEG quality to maintain bandwidth.
- Adaptive framerate to maintain bandwidth.
- Adaptive image compression (for example PNG when bandwidth usage is low, JPEG when high).
- The HTML client decodes the interframes using canvas operations, so on most browsers it should be hardware accelerated.
  - Or optionally, there is code in the script.js to decode using putImageData.
- Passwords using hashtag

## Operation

- Install VLC
- Set the record 'width', 'height', 'offsetX', 'offsetY' in 'ScreenRecorder.java'.
- By default the webserver hosts on port 7578, which is also used in index.html. Change if preferred in 'Config.java'.
- Run 'Main.java'. Read the VLC log and see if everything is okay.
- Open 'index.html' in 'website' dir. Add the hashtag '#put_secret_key_here' at the end. You can change the key or add more in 'FilterHandler.java'.
- Press 'Join the Stream'. If an error appears or video doesn't play, check the Java output. Note that first keyframe doesn't always arrive, so make sure the screen is not still.
- Also make sure no adblockers block your localhost.
- The stream should be playing now =)

## Benchmarks

Only tested on Ubuntu for the Java server.
Desktop PC: Core i5, GTX 970 GPU.
Measurements: at 1920x1080x30fps (interlaced)

- CPU usage is about 18%.
- Bandwidth usage is variable based on scene (still frames have more interframes) and the JPEG quality. Expect 1-2 MB/s.
- Latency is particularly low. My measurements (using Google timer =3) gave me 50ms.
- The Java encoder maxes out at 27-30 fps for me. Since the encoder is practically single threaded, this could very well be improved.
- The HTML client pulls 30fps easily on Chrome. I've tested successfully on my phone as well at lower resolutions.
- HTML client shows tearing on Firefox at 30fps, but only at 1920x1080 and higher. I believe this is because Firefox' canvas isn't hardware 
accelerated on Ubuntu, but I'm not sure. I've testing two decoding implementations and both show it.

## Notes

- There is no sound.
- My implementation had a specific purpose so you'll notice some strange things; streams are encoded more than once for multiple users for example.
However I've stripped a lot of the original code as I didn't think it would benefit anyone else.
- Probably some code smells as this was part of a prototype built in a couple of weeks.
- Little documentation, but the remaining code isn't that elaborate. Most complicated things are in RenderCallback.
- PNG and GIF encoding takes longer, so if the CPU is still busy with the previous frame, the current will be skipped.
This isn't bad when the screen is still, for example when viewing a desktop. At default PNG keyframes are enabled when the screen is still for a long time.
- Bandwidth control happens in the following order:
  - Lowering:
    - PNG -> JPEG
    - JPEG quality--
    - Frameskip++
  - Raising:
    - JPEG quality++
    - Frameskip--
    - JPEG -> PNG
    
I think that's about right, but read the 'Quality.java' and 'RenderCallback' if you wan't to know exactly.
- Interframes take no movement into account. This could decrease bandwidth further, but it was out of scope.

## Used libs

- Webbit websocket server
- VLCj for VLC bindings

## License

GPLv3

## Author

Kaj Toet
