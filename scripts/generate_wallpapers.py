"""Original Qingzhuan wallpapers: analytic soft fields, no external image inputs.
Run with Pillow: python3 scripts/generate_wallpapers.py. CC0-1.0 for generated art.
"""
from pathlib import Path
from PIL import Image
import math
root=Path(__file__).resolve().parents[1]/'android/app/src/main/res/drawable-nodpi'
root.mkdir(parents=True,exist_ok=True)
palettes={
 'mist':((234,241,239),(160,191,211),(179,205,186),(209,217,239)),
 'dune':((247,240,230),(223,185,165),(215,198,219),(233,215,174)),
 'iris':((239,235,246),(189,182,220),(175,200,222),(228,195,212)),
}
for name,(base,a,b,c) in palettes.items():
 data=bytearray()
 for y in range(1280):
  v=y/1279
  for x in range(720):
   u=x/719
   wa=.9*math.exp(-(((u-.15)/.7)**2+((v-.25)/.32)**2)*2)
   wb=.8*math.exp(-(((u-.85)/.65)**2+((v-.8)/.43)**2)*2)
   # Sweeping folded-light contour gives glass a subtle, refractable landmark.
   line=.25+.53*u+.065*math.sin(u*5)
   wc=.75*math.exp(-((v-line)/.067)**2)
   white=.5*math.exp(-((v-line+.07)/.038)**2)
   total=1+wa+wb+wc+white
   data.extend(round((base[k]+a[k]*wa+b[k]*wb+c[k]*wc+255*white)/total) for k in range(3))
 img=Image.frombytes('RGB',(720,1280),bytes(data))
 img.save(root/f'wallpaper_{name}.png',optimize=True)
 img.resize((135,240),Image.Resampling.LANCZOS).save(root/f'wallpaper_{name}_thumb.png',optimize=True)
 print(name,img.size,(root/f'wallpaper_{name}.png').stat().st_size)
