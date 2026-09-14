


## Server safety and storage

Image downloading is server-authoritative. Requests are limited by protocol, MIME type, redirects, byte size, source dimensions, configured target dimensions, timeouts, and public-IP checks that reject loopback, private, link-local, and other non-public destinations. Image data is deduplicated by SHA-256 and stored separately from vanilla map data. Clients receive only requested images in bounded network chunks and keep an LRU texture cache.

The server config controls source-image limits, automatic and manually selectable block-size limits, download size, storage budget, timeout, and optional hostname allow/block lists.

