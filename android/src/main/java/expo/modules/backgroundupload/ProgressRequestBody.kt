package expo.modules.backgroundupload

import okhttp3.MediaType
import okhttp3.RequestBody
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.Sink
import okio.buffer

class ProgressRequestBody(
    private val requestBody: RequestBody,
    private val contentLength: Long,
    private val progressCallback: (bytesUploaded: Long, totalBytes: Long) -> Unit
) : RequestBody() {

    override fun contentType(): MediaType? = requestBody.contentType()

    override fun contentLength(): Long = contentLength

    override fun writeTo(sink: BufferedSink) {
        val progressSink = ProgressSink(sink, contentLength, progressCallback)
        val bufferedSink = progressSink.buffer()
        
        requestBody.writeTo(bufferedSink)
        bufferedSink.flush()
    }

    private class ProgressSink(
        delegate: Sink,
        private val contentLength: Long,
        private val progressCallback: (bytesUploaded: Long, totalBytes: Long) -> Unit
    ) : ForwardingSink(delegate) {
        
        private var bytesWritten = 0L
        private var lastProgressTime = 0L
        private val progressUpdateInterval = 100L // Update every 100ms

        override fun write(source: Buffer, byteCount: Long) {
            super.write(source, byteCount)
            
            bytesWritten += byteCount
            
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastProgressTime >= progressUpdateInterval || bytesWritten >= contentLength) {
                progressCallback(bytesWritten, contentLength)
                lastProgressTime = currentTime
            }
        }
    }
}