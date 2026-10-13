"""Render generated art plus procedural ink-bird flight into a 60 fps scroll film.
Source paintings are immutable; this compositor renders video frames.
"""
from pathlib import Path
import subprocess, json, math, sys, os, shutil
import numpy as np
from PIL import Image, ImageDraw

root=Path(__file__).resolve().parent.parent
out=root/'docs/landing-preview/ink60'
out.mkdir(parents=True,exist_ok=True)
bundled=root/'.tools/video-runtime/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
ffmpeg=os.environ.get('FFMPEG_PATH') or shutil.which('ffmpeg') or (str(bundled) if bundled.exists() else None)
if not ffmpeg: raise SystemExit('Install FFmpeg or set FFMPEG_PATH; Python requires Pillow and numpy.')
W,H,FPS,N=540,960,60,180
def run(args):
    subprocess.run([str(ffmpeg),'-hide_banner','-loglevel','error','-y',*map(str,args)],check=True)
def painting(path):
    return Image.open(path).convert('RGB').resize((W,H),Image.Resampling.LANCZOS)
def clip(start,end,number):
    a,b=np.asarray(painting(start)),np.asarray(painting(end))
    path=out/f'ink-source-{number}.mp4'
    proc=subprocess.Popen([str(ffmpeg),'-hide_banner','-loglevel','error','-y','-f','rawvideo','-pix_fmt','rgb24','-s',f'{W}x{H}','-r',str(FPS),'-i','-','-an','-c:v','libx264','-preset','fast','-crf','20','-pix_fmt','yuv420p','-g','1','-keyint_min','1','-sc_threshold','0','-movflags','+faststart',str(path)],stdin=subprocess.PIPE)
    try:
        for frame in range(N+1):
            t=frame/N
            p=t*t*(3-2*t)
            pixels=np.rint(a.astype(np.float32)*(1-p)+b.astype(np.float32)*p).astype(np.uint8)
            image=Image.fromarray(pixels)
            draw=ImageDraw.Draw(image)
            # Birds enter from outside the painting and leave before the boundary;
            # every clip starts/ends on an unobstructed generated seed painting.
            for bird in range(3):
                u=(t-0.08-bird*0.045)/0.68
                if not 0<u<1: continue
                x=W*(1.10-1.25*u)+bird*7
                y=H*(0.32+0.075*math.sin(u*math.pi))+bird*20
                span=10-bird*1.6
                wing=(4+4*math.sin(frame*0.20+bird*0.65))
                color=(25+bird*10,54+bird*10,61+bird*10)
                left=[(x-span,y-wing),(x-span*0.48,y-wing*0.7),(x,y+1)]
                right=[(x,y+1),(x+span*0.44,y-wing*0.5),(x+span,y-wing*0.85)]
                draw.line(left,fill=color,width=2)
                draw.line(right,fill=color,width=2)
                draw.line([(x-1,y),(x+2,y+3)],fill=color,width=2)
            proc.stdin.write(image.tobytes())
        proc.stdin.close()
        if proc.wait()!=0: raise RuntimeError('Video encoder failed')
    finally:
        if proc.poll() is None: proc.kill()
    run(['-i',path,'-vf',f'select=eq(n\\,{N})','-frames:v','1',out/f'ink-{number}-last-seed.png'])
    print(f'Clip {number}: 181 frames at 60 fps; last seed saved',flush=True)

mode=sys.argv[1]
if mode=='first':
    clip(out/'initial.png',out/'landscape.png',1)
elif mode=='finish':
    clip(out/'ink-1-last-seed.png',out/'open-mist.png',2)
    # Last-frame seed directly establishes the next subtle camera drift.
    source=painting(out/'ink-2-last-seed.png')
    finish=source.resize((W+16,H+28),Image.Resampling.LANCZOS).crop((8,14,W+8,H+14))
    finish.save(out/'final-camera-state.png')
    clip(out/'ink-2-last-seed.png',out/'final-camera-state.png',3)
    codec=['-an','-c:v','libx264','-crf','20','-preset','fast','-pix_fmt','yuv420p','-g','1','-keyint_min','1','-sc_threshold','0','-movflags','+faststart']
    run(['-i',out/'ink-source-1.mp4','-i',out/'ink-source-2.mp4','-i',out/'ink-source-3.mp4','-filter_complex','[1:v]trim=start_frame=1,setpts=PTS-STARTPTS[b];[2:v]trim=start_frame=1,setpts=PTS-STARTPTS[c];[0:v][b][c]concat=n=3:v=1:a=0[v]','-map','[v]','-r',FPS,*codec,out/'ink-journey-60.mp4'])
    report={'fps':FPS,'frames':541,'durationSeconds':541/FPS,'method':'Imagegen paintings; 60 fps composited pigment dissolves, procedural ink-bird flight and subtle camera drift. Not AI-generated object motion.','seams':[]}
    for n,start in enumerate([0,180,360],1):
        run(['-ss',start/FPS,'-i',out/'ink-journey-60.mp4','-frames:v',181,'-c','copy','-movflags','+faststart',out/f'ink-transition-{n}.mp4'])
        for name,frame in [('first',0),('last',180)]:
            run(['-i',out/f'ink-transition-{n}.mp4','-vf',f'select=eq(n\\,{frame})','-frames:v','1',out/f'ink-transition-{n}-{name}.png'])
    for n in [1,2]:
        a=np.asarray(Image.open(out/f'ink-transition-{n}-last.png'))
        b=np.asarray(Image.open(out/f'ink-transition-{n+1}-first.png'))
        equal=bool(np.array_equal(a,b))
        assert equal,'Boundary frames differ'
        report['seams'].append({'afterSegment':n,'pixelIdentical':equal})
    (out/'video-validation.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report),flush=True)
