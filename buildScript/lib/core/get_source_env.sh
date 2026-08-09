# sing-box mod 1.13.x line, which carries:
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
#   - sing-ewp v0.2.6: the EWP services now close their anti-replay cache, so
#     the sweeper goroutine stops with the inbound instead of leaking for the
#     lifetime of the process. v0.2.5 computes the X25519 and ML-KEM-768 halves
#     of the v2.1 hybrid handshake concurrently instead of sequentially (~40%
#     lower handshake crypto latency); wire format and derived keys are
#     unchanged, so no coordinated server upgrade is required. Every
#     handshake error path now wipes both shared-secret halves. v0.2.4
#     flushed buffered transports during the handshake (fixes the EWP
#     handshake EOF over gRPC / XHTTP stream-up).
#   - reality: keep X25519MLKEM768 in ClientHello + correct PQC auth key
#     selection (byte-identical Chrome fingerprint, working REALITY auth)
#   - security: bump deps for GO-2026-5856 (crypto/tls ECH PSK leak),
#     x/net v0.55.0, x/crypto v0.52.0, grpc v1.79.3; requires Go >= 1.25.
#     govulncheck reports 0 reachable vulnerabilities.
# Still on the EWP/v2.1 static-identity API.
export COMMIT_SING_BOX="2f6e8f48befcf5fa1c1db7200ca39733740f990f"
export COMMIT_LIBNEKO="1c47a3af71990a7b2192e03292b4d246c308ef0b"
