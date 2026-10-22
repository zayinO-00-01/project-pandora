"""Composite the generated heron atlas into the clean paintings at 60 fps.

This is an offline video renderer, not work performed on the phone. Requires
Pillow, NumPy and FFmpeg; source paintings/atlas are never modified.
"""
from pathlib import Path
import argparse
import json
import math
import subprocess

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / 'docs/landing-preview/ink60'
FPS, W, H, SS = 60, 540, 960, 3


def smooth(t):
    t = max(0., min(1., t))
    return t*t*(3-2*t)


def pose(seconds, phase=0.):
    # A longer, folded recovery follows the more decisive downstroke.
    p = (seconds*1.55 + phase) % 1.
    angle_phase = math.pi*p/.44 if p < .44 else math.pi+math.pi*(p-.44)/.56
    recovery = 0. if p < .44 else math.sin(math.pi*(p-.44)/.56)**2
    glide = smooth((seconds-1.65)/.24)*(1-smooth((seconds-2.05)/.24))
    angle = math.radians(55*math.cos(angle_phase)-5)
    angle = angle*(1-glide)+math.radians(7)*glide
    fold = math.radians(7+23*recovery)*(1-glide)
    lag = math.radians(5)*math.sin(angle_phase-.35)*(1-glide)
    return angle, fold, lag, angle_phase


class Heron:
    def __init__(self):
        atlas = Image.open(ART/'heron-texture-atlas.png').convert('RGBA')
        # The body toes extend slightly past the atlas's nominal quadrant.
        boxes = [(0, 0, 830, 512), (830, 0, 1536, 512), (0, 512, 768, 1024)]
        self.parts = []
        for box in boxes:
            part = atlas.crop(box)
            part = part.crop(part.getchannel('A').getbbox())
            self.parts.append(part)
        self.body, *self.wings = self.parts
        self.wings = [p.resize((240, 150), Image.Resampling.LANCZOS) for p in self.wings]
        # Include the anatomical joint locations in the tessellation.
        self.us = sorted(set([0, .075, .145, .24, .38, .5, .62, .72, .84, 1] +
            [h+(1-h)*k for h in [.075, .145] for k in [.38, .72]]))
        self.vs = [0, .25, .5, .75, 1]

    @staticmethod
    def joints(seconds, side, phase):
        angle, fold, lag, cycle = pose(seconds, phase)
        points = [np.array([0., side*1.5, 1.])]
        for length, a, yaw in [(13.3, angle, .06),
                               (11.9, angle*.93+lag, fold),
                               (9.8, angle*.84+lag*1.5, fold*1.25+.05)]:
            direction = np.array([-math.sin(yaw)*math.cos(a),
                                  side*math.cos(yaw)*math.cos(a), math.sin(a)])
            points.append(points[-1]+direction*length)
        assert np.allclose(np.linalg.norm(np.diff(points, axis=0), axis=1), [13.3, 11.9, 9.8])
        return points, cycle

    @staticmethod
    def project(point):
        x, y, z = point
        return np.array([-x*.88+y*.475, x*.16+y*.297-z*.94])

    def vertices(self, seconds, side, phase):
        joints, cycle = self.joints(seconds, side, phase)
        knots = [0., .38, .72, 1.]
        result = {}
        hinge_u, hinge_v = (.145, .23) if side == 1 else (.075, .14)
        for u in self.us:
            for v in self.vs:
                q = max(0., (u-hinge_u)/(1-hinge_u))
                segment = min(2, int(np.searchsorted(knots, q, side='right'))-1)
                t = (q-knots[segment])/(knots[segment+1]-knots[segment])
                point = joints[segment]*(1-t)+joints[segment+1]*t
                # The trailing chord stays aft of the same shoulder; flexible
                # outer feather tips lag the powered wing, not the entire bird.
                chord = (v-hinge_v)*19
                point = point + np.array([-chord, 0.,
                    1.8*q**4*v*v*math.sin(cycle-.5)])
                result[u, v] = self.project(point)*SS + np.array([72*SS, 65*SS])
        return result

    @staticmethod
    def triangle(canvas, texture, source, dest):
        dest = np.asarray(dest, dtype=float)
        if abs(np.linalg.det(np.column_stack((dest[1]-dest[0], dest[2]-dest[0])))) < .08:
            return
        left, top = np.floor(dest.min(axis=0)-1).astype(int)
        right, bottom = np.ceil(dest.max(axis=0)+1).astype(int)
        left, top = max(0, left), max(0, top)
        right, bottom = min(canvas.width, right), min(canvas.height, bottom)
        if right <= left or bottom <= top:
            return
        local = dest-np.array([left, top])
        coefficients = np.linalg.solve(np.column_stack((local, np.ones(3))), np.asarray(source)).T.ravel()
        patch = texture.transform((right-left, bottom-top), Image.Transform.AFFINE,
                                  coefficients, Image.Resampling.BICUBIC)
        mask = Image.new('L', patch.size)
        ImageDraw.Draw(mask).polygon([tuple(x) for x in local], fill=255)
        alpha = np.minimum(np.asarray(patch.getchannel('A')), np.asarray(mask))
        patch.putalpha(Image.fromarray(alpha))
        canvas.alpha_composite(patch, (left, top))

    def wing(self, canvas, seconds, side, phase):
        texture = self.wings[0 if side == 1 else 1]
        dest = self.vertices(seconds, side, phase)
        for i in range(len(self.us)-1):
            for j in range(len(self.vs)-1):
                uv = [(self.us[i], self.vs[j]), (self.us[i+1], self.vs[j]),
                      (self.us[i+1], self.vs[j+1]), (self.us[i], self.vs[j+1])]
                src = [(u*(texture.width-1), v*(texture.height-1)) for u, v in uv]
                for indices in [(0, 1, 2), (0, 2, 3)]:
                    self.triangle(canvas, texture, [src[k] for k in indices], [dest[uv[k]] for k in indices])

    def sprite(self, seconds, phase, scale=1., opacity=.8):
        canvas = Image.new('RGBA', (144*SS, 132*SS))
        self.wing(canvas, seconds, -1, phase)
        body = self.body.resize((46*SS, round(46*SS*self.body.height/self.body.width)), Image.Resampling.LANCZOS)
        canvas.alpha_composite(body, (round(72*SS-body.width*.40), round(65*SS-body.height*.43)))
        self.wing(canvas, seconds, 1, phase)
        # A slight bank follows the flight path; no independently spinning parts.
        bank = 1.6*math.sin(seconds*.8)+.5*math.sin(seconds*1.55*math.tau+phase*math.tau)
        canvas = canvas.rotate(bank, Image.Resampling.BICUBIC, center=(72*SS, 65*SS))
        canvas = canvas.resize((round(144*scale), round(132*scale)), Image.Resampling.LANCZOS)
        canvas.putalpha(canvas.getchannel('A').point(lambda a: round(a*opacity)))
        return canvas

    def flock(self, image, seconds):
        # One pass across all transitions, with depth and independent wing phase.
        for dx, dy, scale, opacity, phase in [(0, 0, 1., .80, 0),
                (76, -30, .73, .61, .19), (145, 19, .54, .47, .43)]:
            x = W+86 - 220*(seconds-.35)+dx
            if x < -90 or x > W+90:
                continue
            progress = (W+86-x+dx)/(W+176)
            y = 150+12*math.sin(progress*math.pi)+dy+.7*math.sin(seconds*1.55*math.tau+phase*math.tau)
            sprite = self.sprite(seconds, phase, scale, opacity)
            image.paste(sprite, (round(x-72*scale), round(y-65*scale)), sprite)


def background(paintings, frame):
    segment, offset = min(2, frame//180), frame-min(2, frame//180)*180
    p = smooth(offset/180)
    return Image.fromarray(np.rint(paintings[segment]*(1-p)+paintings[segment+1]*p).astype(np.uint8))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--ffmpeg', default=str(ROOT/'.tools/video-runtime/imageio_ffmpeg/binaries/ffmpeg-win-x86_64-v7.1.exe'))
    parser.add_argument('--preview', action='store_true')
    args = parser.parse_args()
    model = Heron()
    paintings = [np.asarray(Image.open(ART/name).convert('RGB').resize((W, H), Image.Resampling.LANCZOS), dtype=np.float32)
                 for name in ['initial.png', 'landscape.png', 'open-mist.png', 'final-camera-state.png']]
    # Enlarged diagnostic views use the very same animation rig and textures.
    sheet = Image.new('RGB', (900, 600), '#f3efe5')
    for i, t in enumerate([0., .16, .32, .48, .64, 1.85]):
        bird = model.sprite(t, 0, 4., .90)
        bird = bird.crop(bird.getchannel('A').getbbox())
        sheet.paste(bird, (i%3*300+(300-bird.width)//2, i//3*300+(300-bird.height)//2), bird)
    sheet.save(ART/'heron-pose-contact.png')
    for t in [.9, 1.25, 1.8, 2.6, 3.0, 3.4]:
        image = background(paintings, round(t*FPS)); model.flock(image, t)
        image.save(ART/f'heron-frame-{t:.2f}.png')
    if args.preview:
        return
    target = ART/'ink-herons-60.mp4'
    command = [args.ffmpeg, '-hide_banner', '-loglevel', 'error', '-y', '-f', 'rawvideo', '-pix_fmt', 'rgb24',
               '-s', f'{W}x{H}', '-r', str(FPS), '-i', '-', '-an', '-c:v', 'libx264', '-profile:v', 'baseline',
               '-level:v', '3.2', '-preset', 'medium', '-crf', '20', '-maxrate', '3M', '-bufsize', '6M',
               '-pix_fmt', 'yuv420p', '-g', '60', '-keyint_min', '60', '-sc_threshold', '0', '-movflags', '+faststart', str(target)]
    process = subprocess.Popen(command, stdin=subprocess.PIPE)
    try:
        for frame in range(541):
            image = background(paintings, frame)
            model.flock(image, frame/FPS)
            process.stdin.write(image.tobytes())
            if frame % 180 == 0:
                print(f'Rendered {frame}/540', flush=True)
        process.stdin.close()
        if process.wait() != 0:
            raise RuntimeError('Video encoder failed')
    finally:
        if process.poll() is None:
            process.kill()
    report = {'fps': FPS, 'frames': 541, 'durationSeconds': 541/FPS, 'width': W, 'height': H,
              'bytes': target.stat().st_size, 'generatedAtlasUsedInVideo': True,
              'oldLineGlyphsRemoved': True, 'oneContinuousFlockPass': True,
              'wingBones': ['shoulder', 'elbow', 'wrist'], 'wingbeatHz': 1.55,
              'downstrokeFraction': .44, 'recoveryFraction': .56,
              'glideSeconds': [1.65, 2.29], 'supersampling': SS, 'constantBoneLengthAssertions': True}
    (ART/'heron-render-validation.json').write_text(json.dumps(report, indent=2), encoding='utf-8')
    print(json.dumps(report), flush=True)


if __name__ == '__main__':
    main()
