from pathlib import Path
import sys
sys.path.insert(0, str(Path('.tools/font-tools').resolve()))
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset

font = TTFont('C:/Windows/Fonts/NotoSerifSC-VF.ttf')
font = instantiateVariableFont(font, {'wght': 600}, inplace=True)
text = ''.join(p.read_text(encoding='utf-8') for p in Path('android/app/src/main/java').rglob('*.kt'))
options = subset.Options()
options.name_IDs = ['*']
options.name_legacy = True
options.name_languages = ['*']
subsetter = subset.Subsetter(options=options)
subsetter.populate(text=text)
subsetter.subset(font)
target = Path('android/app/src/main/res/font/console_display.ttf')
font.save(target)
assert target.stat().st_size < 1_000_000
print(target.stat().st_size)
