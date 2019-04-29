package distributed.util

import org.json.JSONObject
import java.io.*
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object FileUtil {
    fun zipFile(file: File, itemGroupName: String): ByteArrayOutputStream {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { out ->
            FileInputStream(file).use { fi ->
                BufferedInputStream(fi).use { origin ->
                    val entry = ZipEntry(itemGroupName)
                    out.putNextEntry(entry)
                    origin.copyTo(out, 1024)
                }
            }
        }
        return output
    }

    fun unzipFile(file: ByteArrayInputStream, itemGroupName: String) {
        ZipInputStream(file).use { zis ->
            zis.nextEntry
            val newFile = File(itemGroupName)
            newFile.createNewFile()
            newFile.writeBytes(zis.readBytes())
        }
    }

    fun downloadFile(url: String): ByteArrayInputStream {
        val uri = URL(url)
        return ByteArrayInputStream(uri.readBytes())
    }
}