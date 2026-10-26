package cn.qingzhuan.app

import java.io.*
import java.util.zip.ZipFile
import android.text.Html

object NativePreviewText {
    private fun xml(zip:ZipFile,name:String):org.w3c.dom.Document? {
        val entry=zip.getEntry(name)?:return null
        val bytes=ByteArrayOutputStream();zip.getInputStream(entry).use{Core.copy(it,bytes,4L*1024*1024,Core.NONE)}
        return Core.document(bytes.toByteArray())
    }
    fun read(file:File,english:Boolean=false):String {
        val ext=normalizedSuffix(file.name)
        val text=when(ext) {
            "docx"->ZipFile(file).use{zip->val d=xml(zip,"word/document.xml")?:throw IOException("未找到文档正文");val nodes=d.getElementsByTagNameNS("*","p");(0 until minOf(nodes.length,2000)).joinToString("\n"){nodes.item(it).textContent}.take(40000)}
            "xlsx"->ZipFile(file).use { zip->
                val shared=xml(zip,"xl/sharedStrings.xml")?.getElementsByTagNameNS("*","si")
                val rows=xml(zip,"xl/worksheets/sheet1.xml")?.getElementsByTagNameNS("*","row")?:throw IOException("未找到首个工作表")
                (0 until minOf(rows.length,200)).joinToString("\n") { index->
                    val cells=Array(16){""};var child=rows.item(index).firstChild;var sequential=0
                    while(child!=null){if(child.localName=="c"){
                        val e=child as org.w3c.dom.Element;val letters=e.getAttribute("r").takeWhile{it.isLetter()}.uppercase()
                        val col=if(letters.isEmpty())sequential else if(letters.length>3)999 else letters.fold(0){n,c->n*26+(c-'A'+1)}-1
                        if(col in 0..15){val v=e.getElementsByTagNameNS("*","v").item(0)?.textContent?:"";cells[col]=when(e.getAttribute("t")){"s"->v.toIntOrNull()?.let{if(shared!=null&&it>=0&&it<shared.length)shared.item(it).textContent else ""}?:"";"inlineStr"->e.getElementsByTagNameNS("*","is").item(0)?.textContent?:"";else->v}}
                        sequential++
                    };child=child.nextSibling}
                    cells.toList().dropLastWhile{it.isEmpty()}.joinToString("    │    ")
                }
            }
            "zip"->ZipFile(file).use{zip->val lines=mutableListOf<String>();val entries=zip.entries();while(entries.hasMoreElements()&&lines.size<200){val e=entries.nextElement();lines+=(if(e.isDirectory)"▸ " else "  ")+e.name};if(entries.hasMoreElements())lines+=if(english)"…Showing the first 200 entries"else"…仅展示前200项";lines.joinToString("\n")}
            "txt","md","csv","tsv","json","log","html","htm","xml"->{
                val raw=if(file.length()<=2L*1024*1024)Core.text(file,"auto")else file.inputStream().use{input->val buffer=ByteArray(128*1024);var used=0;while(used<buffer.size){val n=input.read(buffer,used,buffer.size-used);if(n<0)break;used+=n};String(buffer,0,used,Charsets.UTF_8)+if(english)"\n…Preview truncated"else"\n…仅预览开头"}
                if(ext in listOf("html","htm"))Html.fromHtml(raw.replace(Regex("<(style|script)\\b[^>]*>.*?</\\1>",setOf(RegexOption.DOT_MATCHES_ALL,RegexOption.IGNORE_CASE)),""),Html.FROM_HTML_MODE_LEGACY).toString()else raw
            }
            else->throw IOException("此格式暂不支持应用内内容预览。原文件已保留，可直接分享给支持它的应用。")
        }
        return if(text.length>40000)text.take(40000)+if(english)"\n…Preview limited to the first 40,000 characters"else"\n…仅预览前40000字符"else text.ifBlank{if(english)"No readable text found"else"文件没有可显示的文字内容"}
    }
}
