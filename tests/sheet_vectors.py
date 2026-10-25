from pathlib import Path
import json,itertools
R=Path(__file__).resolve().parents[1];geometries=[]
for viewport,natural in itertools.product([240,320,480,640,844,1024,1920],[0,100,280,400,800,3000]):
 maximum=max(1,min(viewport-12,viewport*.94));height=max(min(180,maximum),min(natural or maximum,maximum));collapsed=max(0,height-max(min(320,maximum),viewport*.62))
 assert 0<=collapsed<=height<=viewport
 assert height-collapsed>=min(height,min(320,maximum))
 geometries.append(dict(viewport=viewport,natural=natural,height=height,collapsed=collapsed))
# Explicit intended outcomes, including equality and cancellation boundaries.
release=[]
def case(name,expected,**kw):
 data=dict(start='collapsed',offset=270,velocity=0,delta=0,collapsed=270,height=794,cancelled=False);data.update(kw);release.append(dict(name=name,expected=expected,input=data))
case('tap keeps collapsed','collapsed')
case('short upward jitter keeps snap','collapsed',delta=-5,offset=265)
case('short downward jitter keeps snap','expanded',start='expanded',delta=5,offset=5)
case('slow halfway upward','expanded',offset=100,delta=-170)
case('slow halfway downward','collapsed',start='expanded',offset=200,delta=200)
case('upward fling expands','expanded',offset=230,delta=-40,velocity=-.8)
case('downward fling first collapses expanded','collapsed',start='expanded',offset=60,delta=60,velocity=.8)
case('collapsed down fling dismisses','hidden',offset=310,delta=40,velocity=.8)
case('long pull dismisses','hidden',offset=400,delta=130)
case('upward motion cannot dismiss','expanded',offset=100,delta=-170,velocity=-2)
case('cancelled long pull restores','collapsed',offset=600,delta=330,velocity=3,cancelled=True)
case('cancelled upward restores','collapsed',offset=-10,delta=-280,velocity=-3,cancelled=True)
case('short sheet flick dismisses','hidden',start='expanded',collapsed=0,height=300,offset=50,delta=50,velocity=.8)
case('threshold equality remains collapsed','collapsed',offset=270+794*.13,delta=794*.13)
case('above threshold dismisses','hidden',offset=270+794*.13+.01,delta=794*.13+.01)
(R/'build/sheet-vectors.json').write_text(json.dumps(dict(geometry=geometries,release=release),indent=2))
print('Python verified sheet geometry and snap boundary vectors')
