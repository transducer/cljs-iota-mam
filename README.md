# cljs-iota-mam

[![Clojars Project](https://img.shields.io/clojars/v/cljs-iota-mam.svg)](https://clojars.org/cljs-iota-mam)

ClojureScript API for [IOTA](https://iota.org/) Ledger's [Masked Authenticated Messaging (MAM) JavaScript library](https://github.com/iotaledger/iota.lib.js/).

## Installation

```clojure
(ns my.app
  (:require [cljs-iota-mam.core :as iota-mam]
            [cljs-iota.core :as iota]))
```

Add [`iota-bindings-emscripten.wasm`](https://raw.githubusercontent.com/iotaledger/mam.client.js/master/lib/iota-bindings-emscripten.wasm) to `/public` directory (it is needed by MAM cljsjs).

## TODO

- Properly import MAM file via cljsjs
- Add example

## Usage
Stick with the IOTA MAM JavaScript API [docs](https://github.com/iotaledger/mam.client.js), all methods there have their kebab-cased version in this library. Also, return values and responses in callbacks are automatically kebab-cased and keywordized. Instead of calling a method on the MAM object, you pass it as a first argument. For example:

```javascript
let state = Mam.init(iota, seed, security)
let mode = "restricted"
let sideKey = "SECRET"
let callback = console.log

Mam.changeMode(state, mode, sidekey)
Mam.fetch(root, mode, sidekey, callback)

```
becomes

```clojure
(def state
  (iota-mam/init (iota/create-iota "http://localhost:14700") seed security))

(def mode :restricted)
(def side-key "SECRET")
(def f println)

(iota-mam/change-mode state mode side-key)
(iota-mam/fetch root mode side-key f)
```

Docstrings for the methods and namespaces are adjusted to ClojureScript from the [IOTA MAM JavaScript library](https://github.com/iotaledger/mam.client.js).

## Running tests

Figwheel runs test properly after changing and saving the core_test.cljs file.

## Acknowledgements

This IOTA MAM library uses JavaScript utils methods from the [ClojureScript API for Ethereum Web3 API by district0x](https://github.com/district0x/cljs-web3).
