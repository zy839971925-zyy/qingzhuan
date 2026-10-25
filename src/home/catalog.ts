import type {InputFile,Tool} from '../native/types';
export const TOOLS:Tool[]=[
{id:'image',title:'图片转换',formats:['jpg','png','webp'],description:'透明图片转 JPG 铺白；GIF 只取首帧。'},
{id:'imagepdf',title:'图片成册',formats:['pdf'],description:'按所选文件顺序，每张图片一页。'},
{id:'pdfimage',title:'PDF 转图片',formats:['jpg','png'],description:'选定页面导出为独立图片。'},
{id:'merge',title:'合并 PDF',formats:['pdf'],description:'至少两份 PDF。原数字签名不保留。'},
{id:'split',title:'PDF 拆页',formats:['pdf'],description:'每一页各存为 PDF，可指定页码范围。'},
{id:'document',title:'文档文字转换',formats:['txt','html','docx','pdf'],description:'只提取与重建文字，不保留原版式/图片；扫描件无 OCR。'},
{id:'table',title:'表格转换',formats:['xlsx','csv','tsv','json'],description:'仅首个工作表，不计算公式、不保留样式；JSON 输出为二维数组。'},
{id:'unpack',title:'解压文件',formats:[''],description:'不支持 RAR5、分卷或链接；最多 2000 项 / 1 GiB。'},
{id:'audio',title:'音频提取',formats:['m4a','wav'],description:'M4A 直接提取 AAC；WAV 使用系统解码器。'},
{id:'pack',title:'压缩打包',formats:['zip','tar.gz'],description:'ZIP 可设置 AES 密码；TAR.GZ 不含密码。'},
{id:'hash',title:'文件校验',formats:['txt'],description:'生成 SHA-256 与 MD5 校验文本。'},
];
export const availableTools=(files:InputFile[])=>TOOLS.filter(t=>files.some(f=>f.tools.includes(t.id)));
export function allowedFormats(tool:Tool,files:InputFile[]):string[]{if(tool.id!=='audio')return tool.formats;return tool.formats.filter(f=>files.length>0&&files.every(x=>x.audioTargets?.includes(f)));}
export const size=(n:number)=>n<1024?`${n} B`:n<1048576?`${(n/1024).toFixed(1)} KiB`:`${(n/1048576).toFixed(1)} MiB`;
