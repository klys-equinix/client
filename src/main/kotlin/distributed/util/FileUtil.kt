package distributed.util

import distributed.dto.ItemGroupMetadata
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtil {
    fun zipFile(file: File, itemGroup: ItemGroupMetadata): ByteArrayOutputStream {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { out ->
            FileInputStream(file).use { fi ->
                putZipEntry(fi, out, itemGroup.name)
            }
            ByteArrayInputStream("${itemGroup.id} ${itemGroup.name} ${itemGroup.itemCount}\n".toByteArray(Charset.forName("UTF-8"))).use { fi ->
                putZipEntry(fi, out, "metadata")
            }
            out.closeEntry()
            out.close()
        }
        return output
    }

    fun unzipFile(file: ByteArrayInputStream, itemGroupName: String): ItemGroupMetadata? {
        var itemGroupMetadata: ItemGroupMetadata ?= null
        ZipInputStream(file).use { zis ->
            var entry = zis.nextEntry
            while(entry != null) {
                if (entry.name == "metadata") {
                    val content = zis.readBytes().toString(Charset.forName("UTF-8")).replace("\n", "").split(" ")
                    itemGroupMetadata = ItemGroupMetadata(content[0].toInt(), content[1], content[2].toInt())
                } else {
                    val newFile = File(itemGroupName)
                    newFile.createNewFile()
                    newFile.writeBytes(zis.readBytes())
                }
                entry = zis.nextEntry
            }
            zis.closeEntry()
            zis.close()
        }
        return itemGroupMetadata
    }

    fun downloadFile(url: String): ByteArrayInputStream {
        val uri = URL(url)
        return ByteArrayInputStream(uri.readBytes())
    }

    private fun putZipEntry(fi: InputStream, out: ZipOutputStream, name: String): Long {
        return BufferedInputStream(fi).use { origin ->
            val entry = ZipEntry(name)
            out.putNextEntry(entry)
            origin.copyTo(out, 1024)
        }
    }
}