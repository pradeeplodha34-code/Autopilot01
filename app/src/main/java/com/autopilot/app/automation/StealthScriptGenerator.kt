package com.autopilot.app.automation

import com.autopilot.app.domain.model.DeviceFingerprint
import com.autopilot.app.domain.model.WebRtcPolicy

object StealthScriptGenerator {

    data class StealthPayload(
        val script: String,
        val moduleCount: Int
    )

    fun build(fingerprint: DeviceFingerprint): StealthPayload {
        val langArray = fingerprint.languages.joinToString(",") { "'${escapeJs(it)}'" }
        val webrtcBlock = when (fingerprint.webrtcPolicy) {
            WebRtcPolicy.BLOCK -> """
                try {
                  Object.defineProperty(window, 'RTCPeerConnection', { get: () => undefined });
                  Object.defineProperty(window, 'webkitRTCPeerConnection', { get: () => undefined });
                  if (navigator.mediaDevices) {
                    navigator.mediaDevices.getUserMedia = () => Promise.reject(new DOMException('Permission denied', 'NotAllowedError'));
                  }
                } catch(e) {}
            """.trimIndent()
            WebRtcPolicy.SPOOF -> """
                try {
                  if (window.RTCPeerConnection) {
                    const origRTC = window.RTCPeerConnection;
                    window.RTCPeerConnection = function(cfg, c) {
                      if (cfg && cfg.iceServers) cfg.iceServers = [];
                      return new origRTC(cfg, c);
                    };
                  }
                } catch(e) {}
            """.trimIndent()
            WebRtcPolicy.ALLOW -> ""
        }

        val script = """
            (function() {
              if (window.__autopilotStealthApplied) return;
              window.__autopilotStealthApplied = true;
              try {
                Object.defineProperty(navigator, 'webdriver', { get: () => false });
                Object.defineProperty(navigator, 'platform', { get: () => '${escapeJs(fingerprint.platform)}' });
                Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => ${fingerprint.cpuCores} });
                Object.defineProperty(navigator, 'deviceMemory', { get: () => ${fingerprint.deviceMemory} });
                Object.defineProperty(navigator, 'maxTouchPoints', { get: () => ${fingerprint.maxTouchPoints} });
                Object.defineProperty(navigator, 'languages', { get: () => [$langArray] });
                Object.defineProperty(navigator, 'language', { get: () => '${escapeJs(fingerprint.locale)}' });
              } catch(e) {}
              try {
                Object.defineProperty(screen, 'width', { get: () => ${fingerprint.screenWidth} });
                Object.defineProperty(screen, 'height', { get: () => ${fingerprint.screenHeight} });
                Object.defineProperty(screen, 'availWidth', { get: () => ${fingerprint.screenWidth} });
                Object.defineProperty(screen, 'availHeight', { get: () => ${fingerprint.screenHeight} });
              } catch(e) {}
              try {
                const glProto = WebGLRenderingContext && WebGLRenderingContext.prototype;
                if (glProto && glProto.getParameter) {
                  const origGetParam = glProto.getParameter;
                  glProto.getParameter = function(param) {
                    if (param === 37445) return '${escapeJs(fingerprint.glVendor.ifBlank { "ARM" })}';
                    if (param === 37446) return '${escapeJs(fingerprint.glRenderer.ifBlank { "Mali-G715" })}';
                    return origGetParam.apply(this, arguments);
                  };
                }
              } catch(e) {}
              try {
                const canvasSeed = ${fingerprint.canvasNoiseSeed % 7 + 1};
                const origToDataURL = HTMLCanvasElement.prototype.toDataURL;
                HTMLCanvasElement.prototype.toDataURL = function() {
                  const ctx = this.getContext && this.getContext('2d');
                  if (ctx && this.width > 0 && this.height > 0) {
                    ctx.fillStyle = 'rgba(' + canvasSeed + ',0,0,0.01)';
                    ctx.fillRect(0, 0, 1, 1);
                  }
                  return origToDataURL.apply(this, arguments);
                };
              } catch(e) {}
              $webrtcBlock
            })();
        """.trimIndent()

        val moduleCount = if (fingerprint.webrtcPolicy == WebRtcPolicy.ALLOW) 5 else 6
        return StealthPayload(script = script, moduleCount = moduleCount)
    }

    private fun escapeJs(input: String): String =
        input.replace("\\", "\\\\").replace("'", "\\'")
}
