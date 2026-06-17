package moe.matsuri.nb4a.proxy.ewp

import io.nekohasekai.sagernet.database.DataStore
import io.nekohasekai.sagernet.ktx.linkBuilder
import io.nekohasekai.sagernet.ktx.toLink
import io.nekohasekai.sagernet.ktx.urlSafe
import moe.matsuri.nb4a.SingBoxOptions
import moe.matsuri.nb4a.SingBoxOptions.MultiplexOptions
import moe.matsuri.nb4a.SingBoxOptions.OutboundECHOptions
import moe.matsuri.nb4a.SingBoxOptions.OutboundRealityOptions
import moe.matsuri.nb4a.SingBoxOptions.OutboundTLSOptions
import moe.matsuri.nb4a.SingBoxOptions.OutboundUTLSOptions
import moe.matsuri.nb4a.SingBoxOptions.Outbound_EwpOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_GRPCOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_HTTPOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_HTTPUpgradeOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_WebsocketOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayTransportOptions_XHTTPOptions
import moe.matsuri.nb4a.SingBoxOptions.V2RayXHTTPXmuxOptions
import moe.matsuri.nb4a.utils.listByLineOrComma
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * Build the sing-box outbound option object for an EWP profile.
 *
 * Multiplex is intentionally NOT set here — the global mux logic in
 * ConfigBuilder will inject it via _hack_config_map after the fact,
 * matching how VLESS/Trojan are handled.
 */
fun buildSingBoxOutboundEwpBean(bean: EwpBean): Outbound_EwpOptions {
    return Outbound_EwpOptions().apply {
        type = "ewp"
        server = bean.serverAddress
        server_port = bean.serverPort
        uuid = bean.uuid
        // EWP/v2.1 opt-in: when the server static identity is configured,
        // sing-box will use NewClientV21 and bind the handshake KDF to it.
        if (bean.serverStaticPubKey.isNotBlank()) {
            server_static_public_key = bean.serverStaticPubKey
        }
        tls = buildEwpTLS(bean)
        transport = buildEwpTransport(bean)
    }
}

private fun buildEwpTLS(bean: EwpBean): OutboundTLSOptions {
    return OutboundTLSOptions().apply {
        enabled = true
        insecure = bean.allowInsecure || DataStore.globalAllowInsecure
        if (bean.sni.isNotBlank()) server_name = bean.sni
        if (bean.alpn.isNotBlank()) alpn = bean.alpn.listByLineOrComma()
        if (bean.certificates.isNotBlank()) certificate = bean.certificates

        // TLS fragmentation (anti-censor)
        if (bean.tlsFragment) fragment = true
        if (bean.tlsRecordFragment) record_fragment = true

        // uTLS / Reality
        var fp: String? = bean.utlsFingerprint
        if (bean.realityPubKey.isNotBlank()) {
            reality = OutboundRealityOptions().apply {
                enabled = true
                public_key = bean.realityPubKey
                short_id = bean.realityShortId
            }
            if (fp.isNullOrBlank()) fp = "chrome"
        }
        if (!fp.isNullOrBlank()) {
            utls = OutboundUTLSOptions().apply {
                enabled = true
                fingerprint = fp
            }
        }

        // ECH (with fork-only query_server_name support).
        //
        // REALITY and ECH are mutually exclusive at the protocol level:
        // REALITY needs the outer SNI in cleartext to mimic a real
        // target, ECH encrypts the SNI inside HPKE. sing-box itself
        // silently ignores ECH when REALITY is on (see
        // common/tls/client.go), so we drop ECH here on purpose to
        // make the resulting JSON match what the kernel will actually
        // execute. UI layer also hides ECH when REALITY is set.
        if (bean.enableECH && bean.realityPubKey.isBlank()) {
            ech = OutboundECHOptions().apply {
                enabled = true
                if (bean.echConfig.isNotBlank()) {
                    config = bean.echConfig.lines()
                }
                if (bean.echQueryServerName.isNotBlank()) {
                    query_server_name = bean.echQueryServerName
                }
            }
        }
    }
}

private fun buildEwpTransport(bean: EwpBean): V2RayTransportOptions? {
    return when (bean.type) {
        "tcp", "" -> null

        "ws" -> V2RayTransportOptions_WebsocketOptions().apply {
            type = "ws"
            if (bean.host.isNotBlank()) {
                headers = hashMapOf("Host" to bean.host)
            }
            path = bean.path.takeIf { it.isNotBlank() } ?: "/"
        }

        "http" -> V2RayTransportOptions_HTTPOptions().apply {
            type = "http"
            if (bean.host.isNotBlank()) host = bean.host.split(",")
            path = bean.path.takeIf { it.isNotBlank() } ?: "/"
        }

        "httpupgrade" -> V2RayTransportOptions_HTTPUpgradeOptions().apply {
            type = "httpupgrade"
            host = bean.host
            path = bean.path
        }

        "grpc" -> V2RayTransportOptions_GRPCOptions().apply {
            type = "grpc"
            service_name = bean.path
        }

        "quic" -> V2RayTransportOptions().apply {
            type = "quic"
        }

        "xhttp" -> V2RayTransportOptions_XHTTPOptions().apply {
            type = "xhttp"
            mode = bean.xhttpMode.takeIf { it.isNotBlank() } ?: "auto"
            if (bean.host.isNotBlank()) host = bean.host
            if (bean.path.isNotBlank()) path = bean.path
            if (bean.xhttpPaddingBytes.isNotBlank()) x_padding_bytes = bean.xhttpPaddingBytes

            val hasXmux = bean.xhttpXmuxMaxConcurrency.isNotBlank() ||
                bean.xhttpXmuxMaxConnections.isNotBlank() ||
                bean.xhttpXmuxCMaxReuseTimes.isNotBlank() ||
                bean.xhttpXmuxHMaxRequestTimes.isNotBlank() ||
                bean.xhttpXmuxHMaxReusableSecs.isNotBlank() ||
                (bean.xhttpXmuxHKeepAlivePeriod != null && bean.xhttpXmuxHKeepAlivePeriod > 0)
            if (hasXmux) {
                xmux = V2RayXHTTPXmuxOptions().apply {
                    if (bean.xhttpXmuxMaxConcurrency.isNotBlank()) max_concurrency = bean.xhttpXmuxMaxConcurrency
                    if (bean.xhttpXmuxMaxConnections.isNotBlank()) max_connections = bean.xhttpXmuxMaxConnections
                    if (bean.xhttpXmuxCMaxReuseTimes.isNotBlank()) c_max_reuse_times = bean.xhttpXmuxCMaxReuseTimes
                    if (bean.xhttpXmuxHMaxRequestTimes.isNotBlank()) h_max_request_times = bean.xhttpXmuxHMaxRequestTimes
                    if (bean.xhttpXmuxHMaxReusableSecs.isNotBlank()) h_max_reusable_secs = bean.xhttpXmuxHMaxReusableSecs
                    if (bean.xhttpXmuxHKeepAlivePeriod != null && bean.xhttpXmuxHKeepAlivePeriod > 0) {
                        h_keep_alive_period = bean.xhttpXmuxHKeepAlivePeriod.toLong()
                    }
                }
            }
        }

        else -> null
    }
}

// =================================================================
//                         Link scheme
// =================================================================
//
//   ewp://<uuid>@host:port?type=ws&host=...&path=...&sni=...&alpn=...
//        &fp=chrome&insecure=0&ech=1&echQs=public.example
//        #remarks
//
// Mirrors the ad-hoc style used by AnyTLS / VLESS share links.

fun EwpBean.toUri(): String {
    val builder = linkBuilder()
        .username(uuid)
        .host(serverAddress)
        .port(serverPort)

    if (!name.isNullOrBlank()) {
        builder.encodedFragment(name.urlSafe())
    }

    // EWP/v2.1: server static identity public key (base64 X25519).
    if (serverStaticPubKey.isNotBlank()) builder.addQueryParameter("sk", serverStaticPubKey)

    if (type.isNotBlank() && type != "tcp") builder.addQueryParameter("type", type)
    if (host.isNotBlank()) builder.addQueryParameter("host", host)
    if (path.isNotBlank()) builder.addQueryParameter("path", path)

    if (type == "xhttp" && xhttpMode.isNotBlank() && xhttpMode != "auto") {
        builder.addQueryParameter("mode", xhttpMode)
    }

    if (sni.isNotBlank()) builder.addQueryParameter("sni", sni)
    if (alpn.isNotBlank()) builder.addQueryParameter("alpn", alpn)
    if (utlsFingerprint.isNotBlank()) builder.addQueryParameter("fp", utlsFingerprint)
    if (allowInsecure) builder.addQueryParameter("insecure", "1")

    if (enableECH) {
        builder.addQueryParameter("ech", "1")
        if (echConfig.isNotBlank()) builder.addQueryParameter("echCfg", echConfig.replace("\n", "|"))
        if (echQueryServerName.isNotBlank()) builder.addQueryParameter("echQs", echQueryServerName)
    }

    if (tlsFragment) builder.addQueryParameter("frag", "1")
    if (tlsRecordFragment) builder.addQueryParameter("rfrag", "1")

    return builder.toLink("ewp")
}

fun parseEwp(url: String): EwpBean {
    val link = url.replace("ewp://", "https://").toHttpUrlOrNull()
        ?: error("invalid ewp link $url")
    return EwpBean().apply {
        serverAddress = link.host
        serverPort = link.port
        name = link.fragment
        uuid = link.username

        // EWP/v2.1: server static identity public key (base64 X25519).
        serverStaticPubKey = link.queryParameter("sk") ?: ""

        type = link.queryParameter("type") ?: "tcp"
        host = link.queryParameter("host") ?: ""
        path = link.queryParameter("path") ?: ""

        if (type == "xhttp") {
            xhttpMode = link.queryParameter("mode") ?: "auto"
        }

        sni = link.queryParameter("sni") ?: ""
        alpn = link.queryParameter("alpn") ?: ""
        utlsFingerprint = link.queryParameter("fp") ?: ""
        link.queryParameter("insecure")?.also {
            allowInsecure = it == "1" || it == "true"
        }

        link.queryParameter("ech")?.also {
            enableECH = it == "1" || it == "true"
        }
        echConfig = link.queryParameter("echCfg")?.replace("|", "\n") ?: ""
        echQueryServerName = link.queryParameter("echQs") ?: ""

        link.queryParameter("frag")?.also {
            tlsFragment = it == "1" || it == "true"
        }
        link.queryParameter("rfrag")?.also {
            tlsRecordFragment = it == "1" || it == "true"
        }
    }
}
