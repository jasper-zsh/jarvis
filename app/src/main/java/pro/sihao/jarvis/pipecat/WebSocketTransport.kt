package pro.sihao.jarvis.pipecat

import ai.pipecat.client.result.Future
import ai.pipecat.client.result.RTVIError
import ai.pipecat.client.result.resolvedPromiseErr
import ai.pipecat.client.result.resolvedPromiseOk
import ai.pipecat.client.small_webrtc_transport.SmallWebRTCTransport
import ai.pipecat.client.transport.MsgClientToServer
import ai.pipecat.client.transport.Transport
import ai.pipecat.client.transport.TransportContext
import ai.pipecat.client.types.APIRequest
import ai.pipecat.client.types.MediaDeviceId
import ai.pipecat.client.types.MediaDeviceInfo
import ai.pipecat.client.types.Tracks
import ai.pipecat.client.types.TransportState
import ai.pipecat.client.utils.ThreadRef
import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json
import java.util.Collections

internal val JSON_INSTANCE = Json { ignoreUnknownKeys = true }

data class WebSocketTransportConnectParams(
    val wsUrl: String
)

class WebSocketTransport(
    context: Context
) : Transport<WebSocketTransportConnectParams>() {

    companion object {
        private const val TAG = "WebSocketTransport"
    }

    object AudioDevices {
        val Glasses = MediaDeviceInfo(
            id = MediaDeviceId("glasses"),
            name = "Glasses"
        )

        val Speakerphone = MediaDeviceInfo(
            id = MediaDeviceId("speakerphone"),
            name = "Speakerphone"
        )
    }

    private lateinit var transportContext: TransportContext
    private lateinit var thread: ThreadRef

    private var state = TransportState.Disconnected

    override fun initialize(ctx: TransportContext) {
        transportContext = ctx
        thread = ctx.thread
    }

    override fun deserializeConnectParams(
        json: String,
        startBotRequest: APIRequest
    ): WebSocketTransportConnectParams {
        return JSON_INSTANCE.decodeFromString<WebSocketTransportConnectParams>(json)
    }

    override fun initDevices(): Future<Unit, RTVIError> {
        return resolvedPromiseOk(thread, Unit)
    }

    override fun release() {
        disconnect().logError(TAG, "Disconnect triggered by release() failed")
    }

    override fun connect(transportParams: WebSocketTransportConnectParams): Future<Unit, RTVIError> {
        TODO("Not yet implemented")
    }

    override fun disconnect(): Future<Unit, RTVIError> {
        TODO("Not yet implemented")
    }

    override fun getAllMics(): Future<List<MediaDeviceInfo>, RTVIError> {
        return resolvedPromiseOk(thread, listOf(AudioDevices.Glasses, AudioDevices.Speakerphone))
    }

    override fun getAllCams(): Future<List<MediaDeviceInfo>, RTVIError> {
        return resolvedPromiseOk(thread, emptyList())
    }

    override fun updateMic(micId: MediaDeviceId): Future<Unit, RTVIError> {
        TODO("Not yet implemented")
    }

    override fun updateCam(camId: MediaDeviceId): Future<Unit, RTVIError> {
        return resolvedPromiseOk(thread, Unit)
    }

    override fun selectedMic(): MediaDeviceInfo? {
        TODO("Not yet implemented")
    }

    override fun selectedCam(): MediaDeviceInfo? {
        return null
    }

    override fun enableMic(enable: Boolean): Future<Unit, RTVIError> {
        TODO("Not yet implemented")
    }

    override fun enableCam(enable: Boolean): Future<Unit, RTVIError> {
        return resolvedPromiseErr(thread, RTVIError.OtherError("Cam not supported yet"))
    }

    override fun isCamEnabled(): Boolean {
        return false
    }

    override fun isMicEnabled(): Boolean {
        TODO("Not yet implemented")
    }

    override fun sendMessage(message: MsgClientToServer): Future<Unit, RTVIError> {
        TODO("Not yet implemented")
    }

    override fun state(): TransportState {
        return state
    }

    override fun setState(state: TransportState) {
        Log.i(TAG, "setState($state)")
        thread.assertCurrent()
        this.state = state
        transportContext.callbacks.onTransportStateChanged(state)
    }

    override fun tracks(): Tracks {
        TODO("Not yet implemented")
    }
}