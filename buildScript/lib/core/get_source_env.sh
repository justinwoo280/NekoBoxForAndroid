# sing-box mod 1.13.x line
# Pinned to the 1.13.x commit carrying sing-ewp v0.2.4, which flushes
# buffered transports after each handshake message (fixes the EWP
# handshake EOF over gRPC / XHTTP stream-up). Still on the EWP/v2.1
# static-identity API.
export COMMIT_SING_BOX="f2392d5936dbe8d757413767320881a400764f15"
export COMMIT_LIBNEKO="1c47a3af71990a7b2192e03292b4d246c308ef0b"
