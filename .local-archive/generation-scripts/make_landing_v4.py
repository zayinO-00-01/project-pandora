from pathlib import Path
import subprocess, json, sys
root=Path(__file__).resolve().parent.parent
out=root/'docs/landing-preview'
ffmpeg=root/'.tools/video-runtime/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'
def run(args):subprocess.run([str(ffmpeg),'-hide_banner','-loglevel','error','-y',*map(str,args)],check=True)
codec=['-r','30','-fps_mode','cfr','-enc_time_base','1:30','-an','-c:v','libx264','-crf','18','-pix_fmt','yuv420p','-g','1','-keyint_min','1','-sc_threshold','0']
def dissolve(a,b,name):
    t='min(1,max(0,(T-0.3)/1.1))'
    ease=f'(3*pow({t},2)-2*pow({t},3))'
    run(['-loop','1','-framerate','30','-i',a,'-loop','1','-framerate','30','-i',b,'-filter_complex',f"[0:v]scale=720:960,setsar=1[a];[1:v]scale=720:960,setsar=1[b];[a][b]blend=all_expr='A*(1-{ease})+B*{ease}'",'-frames:v','91',*codec,out/name])
if sys.argv[1]=='second':
    dissolve(out/'transition-1-last-v4.png',out/'open-journal-v4.png','transition-2-source-v4.mp4')
    run(['-i',out/'transition-2-source-v4.mp4','-vf','select=eq(n\\,90)','-frames:v','1',out/'transition-2-last-v4.png'])
else:
    dissolve(out/'transition-2-last-v4.png',out/'unfolded-journal-v4.png','transition-3-source-v4.mp4')
    run(['-i',out/'transition-1-source-v4.mp4','-i',out/'transition-2-source-v4.mp4','-i',out/'transition-3-source-v4.mp4','-filter_complex','[1:v]trim=start_frame=1,setpts=PTS-STARTPTS[b];[2:v]trim=start_frame=1,setpts=PTS-STARTPTS[c];[0:v][b][c]concat=n=3:v=1:a=0[v0];[v0]tpad=stop_mode=clone:stop_duration=0.1,fps=30,trim=end_frame=271,setpts=N/(30*TB)[v]','-map','[v]',*codec,'-movflags','+faststart',out/'landing-journey-v4.mp4'])
    report={'technique':'Generated keyframes; continuous camera and painterly dissolves. No AI object deformation.','frames':271,'fps':30,'seams':[]}
    for n,(start,end) in enumerate([(0,90),(90,180),(180,270)],1):
        clip=out/f'transition-{n}-v4.mp4'
        run(['-ss',str(start/30),'-i',out/'landing-journey-v4.mp4','-frames:v',str(end-start+1),'-c','copy','-movflags','+faststart',clip])
        for label,frame in [('first',0),('last',90)]:run(['-i',clip,'-vf',f'select=eq(n\\,{frame})','-frames:v','1',out/f'transition-{n}-{label}-v4.png'])
    for n in [1,2]:
        identical=(out/f'transition-{n}-last-v4.png').read_bytes()==(out/f'transition-{n+1}-first-v4.png').read_bytes()
        report['seams'].append({'between':[n,n+1],'pixelIdentical':identical})
        assert identical
    (out/'video-validation-v4.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report))




