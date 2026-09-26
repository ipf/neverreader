package com.neverreader.util.java

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.io.PushbackInputStream
import java.io.UnsupportedEncodingException
import java.io.Writer

object UnicodeUtils {
    @Throws(Exception::class)
    fun convert(bytes: ByteArray, encout: String?): ByteArray {
        // Workaround for bug that will not be fixed by SUN
        // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4508058
        val uis = UnicodeInputStream(ByteArrayInputStream(bytes), "ASCII")
        val unicodeOutputReqd = if (getBOM(encout) != null) true else false
        val enc = uis.getEncoding()
        var BOM = getBOM(enc) // get the BOM of the inputstream

        if (BOM == null) {
            // inputstream looks like ascii...
            // create a BOM based on the outputstream
            BOM = getBOM(encout)
        }
        uis.close()

        val out = ByteArrayOutputStream()
        val br = BufferedReader(
            InputStreamReader(
                ByteArrayInputStream(
                    bytes,
                    uis.bOMOffset, bytes.size
                ), enc
            )
        )
        val w: Writer = BufferedWriter(OutputStreamWriter(out, encout))

        // dont write a BOM for ascii(out) as the OutputStreamWriter
        // will not process it correctly.
        if (BOM != null && unicodeOutputReqd) {
            w.write(BOM)
        }

        val buffer = CharArray(4096)
        var len: Int
        while ((br.read(buffer).also { len = it }) != -1) {
            w.write(buffer, 0, len)
        }

        br.close() // Close the input.
        w.close() // Flush and close output.
        return out.toByteArray()
    }

    @Throws(UnsupportedEncodingException::class)
    fun getBOM(enc: String?): String? {
        if ("UTF-8" == enc) {
            val bom = ByteArray(3)
            bom[0] = 0xEF.toByte()
            bom[1] = 0xBB.toByte()
            bom[2] = 0xBF.toByte()
            return String(bom, charset(enc))
        } else if ("UTF-16BE" == enc) {
            val bom = ByteArray(2)
            bom[0] = 0xFE.toByte()
            bom[1] = 0xFF.toByte()
            return String(bom, charset(enc))
        } else if ("UTF-16LE" == enc) {
            val bom = ByteArray(2)
            bom[0] = 0xFF.toByte()
            bom[1] = 0xFE.toByte()
            return String(bom, charset(enc))
        } else if ("UTF-32BE" == enc) {
            val bom = ByteArray(4)
            bom[0] = 0x00.toByte()
            bom[1] = 0x00.toByte()
            bom[2] = 0xFE.toByte()
            bom[3] = 0xFF.toByte()
            return String(bom, charset(enc))
        } else if ("UTF-32LE" == enc) {
            val bom = ByteArray(4)
            bom[0] = 0x00.toByte()
            bom[1] = 0x00.toByte()
            bom[2] = 0xFF.toByte()
            bom[3] = 0xFE.toByte()
            return String(bom, charset(enc))
        } else {
            return null
        }
    }

    class UnicodeInputStream(`in`: InputStream?, private val defaultEnc: String?) : InputStream() {
        private val internalIn: PushbackInputStream

        private var isInited = false

        var bOMOffset: Int = -1
            private set

        private var encoding: String? = null

        init {
            internalIn = PushbackInputStream(`in`, BOM_SIZE)
        }

        fun getEncoding(): String? {
            if (!isInited) {
                try {
                    init()
                } catch (ex: IOException) {
                    val ise = IllegalStateException("Init method failed.")
                    ise.initCause(ise)
                    throw ise
                }
            }
            return encoding
        }

        /**
         * Read-ahead four bytes and check for BOM marks. Extra bytes are unread
         * back to the stream, only BOM bytes are skipped.
         */
        @Throws(IOException::class)
        protected fun init() {
            if (isInited) return

            val bom: ByteArray? = ByteArray(BOM_SIZE)
            val n: Int
            val unread: Int
            n = internalIn.read(bom, 0, bom!!.size)

            if ((bom[0] == 0x00.toByte()) && (bom[1] == 0x00.toByte()) && (bom[2] == 0xFE.toByte()) && (bom[3] == 0xFF.toByte())) {
                encoding = "UTF-32BE"
                unread = n - 4
            } else if ((bom[0] == 0xFF.toByte()) && (bom[1] == 0xFE.toByte()) && (bom[2] == 0x00.toByte()) && (bom[3] == 0x00.toByte())) {
                encoding = "UTF-32LE"
                unread = n - 4
            } else if ((bom[0] == 0xEF.toByte()) && (bom[1] == 0xBB.toByte()) && (bom[2] == 0xBF.toByte())) {
                encoding = "UTF-8"
                unread = n - 3
            } else if ((bom[0] == 0xFE.toByte()) && (bom[1] == 0xFF.toByte())) {
                encoding = "UTF-16BE"
                unread = n - 2
            } else if ((bom[0] == 0xFF.toByte()) && (bom[1] == 0xFE.toByte())) {
                encoding = "UTF-16LE"
                unread = n - 2
            } else {
                // Unicode BOM mark not found, unread all bytes
                encoding = defaultEnc
                unread = n
            }
            this.bOMOffset = BOM_SIZE - unread
            if (unread > 0) internalIn.unread(bom, (n - unread), unread)

            isInited = true
        }

        @Throws(IOException::class)
        override fun close() {
            // init();
            isInited = true
            internalIn.close()
        }

        @Throws(IOException::class)
        override fun read(): Int {
            // init();
            isInited = true
            return internalIn.read()
        }

        companion object {
            const val BOM_SIZE: Int = 4
        }
    }
}
