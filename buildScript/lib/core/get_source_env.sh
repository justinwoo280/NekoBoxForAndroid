# sing-box mod 1.13.x line — pinned to v1.13.11-mod.5, which carries:
#   - sing-ewp v0.2.4: flush buffered transports during handshake (fixes
#     the EWP handshake EOF over gRPC / XHTTP stream-up)
#   - reality: keep X25519MLKEM768 in ClientHello + correct PQC auth key
#     selection (byte-identical Chrome fingerprint, working REALITY auth)
# Still on the EWP/v2.1 static-identity API.
export COMMIT_SING_BOX="6d33f8ae534b69014888af6f45bf7e8fcec969ef"
export COMMIT_LIBNEKO="1c47a3af71990a7b2192e03292b4d246c308ef0b"
