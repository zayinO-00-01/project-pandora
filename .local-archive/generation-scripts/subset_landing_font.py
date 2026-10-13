import sys
from pathlib import Path
sys.path.insert(0,str(Path('.tools/font-runtime').resolve()))
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont
from fontTools import subset
font=TTFont('.tools/noto_serif_sc.ttf')
font=instantiateVariableFont(font,{'wght':500},inplace=True)
options=subset.Options();options.name_IDs=['*'];options.name_legacy=True;options.name_languages=['*']
subsetter=subset.Subsetter(options)
subsetter.populate(text=Path('android/app/src/main/java/com/projectpandora/app/ui/LandingJourney.kt').read_text(encoding='utf-8')+'潘多拉让每一步工作都有迹可循从页记录理清下让分散的回到条时间线把今天写来。 ，、0123456789年月日 /↗')
subsetter.subset(font)
font.save('android/app/src/main/res/font/landing_serif.ttf')
