# Certificates

Put the public certificate of the CA that signed Logto's TLS certificate here as `logto-ca.pem`, for example
the mkcert root: `cat "$(mkcert -CAROOT)/rootCA.pem"` on the machine that created it. Never copy
`rootCA-key.pem`. Compose mounts the file into every container as `/run/secrets/logto-ca`, and services trust it
for calls to Logto when `LOGTO_CA_CERT=/run/secrets/logto-ca` is set.

`*.pem` files here are git-ignored: the certificate is public, but it belongs to one homelab.
