# sing-box mod 1.13.x line, which carries:
#   - xhttp: transport delegated to the standalone sing-xhttp v0.1.2 library
#     (full Xray splithttp parity, REALITY/HTTP3/uTLS, per-mode defaults);
#     in-tree transport/v2rayxhttp is now a thin bridge. v0.1.1 fixes the
#     packet-up/stream-* deadlock through response-buffering middleboxes
#     (Cloudflare) by returning the download GET at connection time.
#   - sing-ewp v0.2.4: flush buffered transports during handshake (fixes
#     the EWP handshake EOF over gRPC / XHTTP stream-up)
#   - reality: keep X25519MLKEM768 in ClientHello + correct PQC auth key
#     selection (byte-identical Chrome fingerprint, working REALITY auth)
#   - security: bump deps for GO-2026-5856 (crypto/tls ECH PSK leak),
#     x/net v0.55.0, x/crypto v0.52.0, grpc v1.79.3; requires Go >= 1.25.
#     govulncheck reports 0 reachable vulnerabilities.
# Still on the EWP/v2.1 static-identity API.
export COMMIT_SING_BOX="31d96df4746ed7fd64f068a0e7be18765ea2a50d"
export COMMIT_LIBNEKO="1c47a3af71990a7b2192e03292b4d246c308ef0b"
