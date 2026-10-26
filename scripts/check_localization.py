"""Check the catalog and actual pure Java matcher. Does not translate user content."""
from pathlib import Path
import json,re,subprocess,tempfile
from jdk_tools import jdk_bin
root=Path(__file__).resolve().parents[1]
data=json.loads((root/'native-assets/strings_en.json').read_text())
missing=[]
for p in (root/'android/app/src/main/java').rglob('*.kt'):
 if p.name in ['ReferenceOptics.kt','NativeMediaStore.kt','NativePreviewText.kt','UiText.kt']:continue
 for raw in re.findall(r'"((?:[^"\\]|\\.)*)"',p.read_text()):
  if not re.search('[\u4e00-\u9fff]',raw) or '$' in raw or '%' in raw:continue
  if raw not in data['exact'] and raw not in ['Jetpack Compose 原生首页','原生文件引擎初始化','文件引擎初始化']:missing.append((p.name,raw))
assert not missing,missing
assert all(value.strip() for value in data['exact'].values())
jdk=jdk_bin(root)
def literal(s):return json.dumps(s,ensure_ascii=False)
source='import cn.qingzhuan.app.TextCatalog;import java.util.*;public class CatalogSmoke{public static void main(String[]args){Map<String,String> m=new HashMap<>();List<String[]> r=new ArrayList<>();'
source+=''.join('m.put('+literal(k)+','+literal(v)+');' for k,v in data['exact'].items())
source+=''.join('r.add(new String[]{'+literal(v['re'])+','+literal(v['en'])+','+literal('cause' if v.get('cause') else '')+'});' for v in data['rules'])
source+='TextCatalog c=new TextCatalog(m,r);'
cases=[('滑动以开始转换','Swipe to convert'),('原始尺寸 · 最高质量','Original size · Maximum quality'),('保存 设置.png','Save 设置.png'),('深色遮罩 · 72%','Dark overlay · 72%'),('删除 3 项？','Delete 3 items?'),('第12页','Page 12'),('文件名未知123.png','文件名未知123.png'),('模糊 · 18','Blur · 18'),('视频编码失败：没有找到音轨','Video encoding failed: No audio track found')]
for a,b in cases:source+='if(!c.translate('+literal(a)+').equals('+literal(b)+'))throw new AssertionError('+literal(a)+');'
source+='System.out.println("9 actual Java catalog checks + static UI catalog coverage passed");}}'
with tempfile.TemporaryDirectory() as tmp:
 p=Path(tmp)/'CatalogSmoke.java';p.write_text(source)
 subprocess.run([str(jdk/'javac'),'-d',tmp,str(root/'app/src/main/java/cn/qingzhuan/app/TextCatalog.java'),str(p)],check=True)
 subprocess.run([str(jdk/'java'),'-cp',tmp,'CatalogSmoke'],check=True)

for name in ['NativeHome.kt','NativeSheets.kt','ImagePreviewDeck.kt','NativePreviewActivity.kt']:
 p=next((root/'android/app/src/main/java').rglob(name));source=p.read_text()
 assert not re.search(r'UiText\(\s*(?:completed\.first\(\)|f|focused|file)\.name',source),(name,'filename passed to translation')
 assert 'CustomAccessibilityAction("直接开始转换")' not in source
for name in ['Core','Engine','AppSession','NativeMediaEngine','MediaOptions','MotionPhoto','ImageTransformPolicy']:
 source=(root/'app/src/main/java/cn/qingzhuan/app'/f'{name}.java').read_text()
 for raw in re.findall(r'throw new (?:IOException|IllegalArgumentException|IllegalStateException)\("([^"\n]+)"\)',source):
  if re.search('[\u4e00-\u9fff]',raw):assert raw in data['exact'],(name,raw)
print('Native static error coverage and filename/accessibility boundary checks passed')
