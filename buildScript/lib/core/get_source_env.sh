# sing-box mod 1.13.x line, which carries:
#   - EWP/v2.3 (sing-ewp v0.3.0): six-stage authenticated handshake with
#     full transcript binding, Ed25519 server signing identity + signed
#     short-term outer keys (replacing the v2.1 static X25519 server key),
#     hybrid X25519+ML-KEM-768 session keys, stateless cookie + admission
#     control, in-window replay rejection, AnyTLS-style opening-phase
#     refragmentation, and async TCP/UDP handoff fixes (gRPC/xhttp).
#     Config: outbound takes uuid + server_public_key + server_id
#     (+ route_epoch); inbound takes users + signing_private_key + server_id.
#   - xhttp: transport delegated to the standalone sing-xhttp v0.1.6 library
#     (full Xray splithttp parity, REALITY/HTTP3/uTLS, per-mode defaults);
#     in-tree transport/v2rayxhttp is now a thin bridge. v0.1.6 rejects
#     `alpn: ["h3"]` combined with uTLS or REALITY instead of silently falling
#     back to HTTP/2 over TCP: quic-go runs the TLS 1.3 handshake through
#     crypto/tls and exposes no hook for a caller-supplied ClientHello, so
#     HTTP/3 needs a standard TLS client. v0.1.5 completes the
#     Chromium-like H2 client frame adaptation without forking x/net/http2:
#     SETTINGS order/presence, session WINDOW_UPDATE, HEADERS priority,
#     RFC 9218 priority header, and request pseudo-header order are aligned;
#     client-side only, wire format unchanged. v0.1.1 fixes the packet-up/
#     stream-* deadlock through response-buffering middleboxes (Cloudflare) by
#     returning the download GET at connection time.
#   - reality: keep X25519MLKEM768 in ClientHello + correct PQC auth key
#     selection (byte-identical Chrome fingerprint, working REALITY auth)
#   - security: bump deps for GO-2026-5856 (crypto/tls ECH PSK leak),
#     x/net v0.55.0, x/crypto v0.52.0, grpc v1.79.3; requires Go >= 1.25.
#     govulncheck reports 0 reachable vulnerabilities.
export COMMIT_SING_BOX="5f6bd3b1367568578daaf65294a8877ff39fd497"
export COMMIT_LIBNEKO="1c47a3af71990a7b2192e03292b4d246c308ef0b"
