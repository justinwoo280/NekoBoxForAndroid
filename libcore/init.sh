#!/bin/bash

chmod -R 777 .build 2>/dev/null
rm -rf .build 2>/dev/null

if [ -z "$GOPATH" ]; then
    GOPATH=$(go env GOPATH)
fi

# Install stock gomobile + gobind (golang.org/x/mobile).
if [ ! -f "$GOPATH/bin/gomobile" ]; then
    go install golang.org/x/mobile/cmd/gomobile@latest
fi
if [ ! -f "$GOPATH/bin/gobind" ]; then
    go install golang.org/x/mobile/cmd/gobind@latest
fi

export PATH="$GOPATH/bin:$PATH"
gomobile init
