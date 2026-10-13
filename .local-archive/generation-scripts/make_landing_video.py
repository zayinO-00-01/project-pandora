"""Render an actual image-based camera film; shared boundary frames seed each clip."""
from pathlib import Path
import subprocess,json,hashlib
root=Path(__file__).resolve().parent.parent
ffmpeg=root/'.tools/video-runtime/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
out=root/'docs/landing-preview'
def run(args):
    subprocess.run([str(ffmpeg),'-hide_banner','-loglevel','error','-y',*map(str,args)],check=True)
# One continuous C1-smooth camera path, 9 seconds / 270 intervals / 271 frames.
# Crop travels toward the open folio while the lens eases inward. No generated deformation.
zoom="1+0.38*(3*pow(on/270,2)-2*pow(on/270,3))"
vf=f"scale=2160:2880,zoompan=z='{zoom}':x='(iw-iw/zoom)*(0.5-0.23*on/270)':y='(ih-ih/zoom)*(0.5+0.45*on/270)':d=271:s=720x960:fps=30"
codec=['-an','-fps_mode','passthrough','-c:v','libx264','-preset','medium','-crf','18','-pix_fmt','yuv420p','-g','1','-keyint_min','1','-sc_threshold','0','-movflags','+faststart']
run(['-i',out/'initial-frame-v2.png','-vf',vf,'-frames:v','271',*codec,out/'landing-journey-v2.mp4'])
report={'technique':'Image-generated first frame, deterministic camera film; not AI object animation','fps':30,'frames':271,'durationSeconds':271/30,'segments':[]}
for n,(start,end) in enumerate([(0,90),(90,180),(180,270)],1):
    # Start each new segment from the preceding last frame, not an approximation.
    clip=out/f'transition-{n}-v2.mp4'
    run(['-ss',str(start/30),'-i',out/'landing-journey-v2.mp4','-frames:v',str(end-start+1),'-c','copy','-movflags','+faststart',clip])
    for label,frame in [('first',0),('last',end-start)]:
        run(['-i',clip,'-vf',f'select=eq(n\\,{frame})','-frames:v','1',out/f'transition-{n}-{label}.png'])
    report['segments'].append({'clip':clip.name,'startFrame':start,'endFrame':end})
for n in [1,2]:
    a=out/f'transition-{n}-last.png';b=out/f'transition-{n+1}-first.png'
    identical=a.read_bytes()==b.read_bytes()
    report.setdefault('seams',[]).append({'between':[n,n+1],'pixelIdentical':identical})
    assert identical,'A segment boundary changed pixels'
(out/'video-validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
run(['-i',out/'landing-journey-v2.mp4',out/'frames/frame-%03d.png'])
print(json.dumps(report,ensure_ascii=False))

