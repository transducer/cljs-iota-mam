(ns cljs-iota-mam.core
  "ClojureScript wrapper around IOTA Masked Authenticated Messaging (MAM)
  JavaScript API methods on the IOTA object.

  An `iota` instance can be obtained via `cljs-iota/create-iota` in the
  cljs-iota library.

  ```
  (def iota-instance
    (cljs-iota/create-iota \"http://localhost\" 14265))
  ```

  A `mam` instance can be obtained via `init`.

  (def mam-instance
    (cljs-iota-mam/init iota nil 2))
  ```"
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-iota-mam.js-utils :as js-utils]
            [cljs-iota.utils :as iota-utils]
            [cljs.core.async :as async :refer [>! chan]]
            cljsjs.iota-mam
            [taoensso.timbre :as log]))

;;;;
;;;; Basic Usage


(defn init
  "This takes initialises the state and binds the `iota.lib.js` to the library.
  This will return a state object that tracks the progress of your stream and
  streams you are following.

  Arguments:
  iota - object Initialised IOTA library with a provider set.
  seed - string Tryte-encoded seed. `nil` value generates a random seed.
  security - int Security of the keys used. `nil` value defaults to 2.

  ;; TODO:
  start-count - int Start index for reading messages. `nil` value defaults to 0.

  Returns initialised state object to be used in future actions."
  [iota seed security #_start-count]
  (.init js/Mam iota seed security #_start-count))


(defn change-mode
  "This takes the state object and changes the default stream mode from public
  to the specified mode and `side-key`. There are only three possible modes:
  `:public`, `:private`, & `:restricted`. If you fail to pass one of these modes
  it will default to `:public`. This will return a state object that tracks the
  progress of your stream and streams you are following.

  Arguments:
  state - object Initialised IOTA MAM library with a provider set.
  mode - keyword Intended channel mode. Can be only: `:public`, `:private` or
                 `:restricted`.

  side-key - string Tryte-encoded encryption key, any length. Required for
                    `:restricted` mode.

  Returns initialised JavaScript state object to be used in future actions"
  [state mode side-key]
  (let [js-state (js-utils/mam-state-to-js state)]
    (-> (.changeMode js/Mam js-state (name mode) side-key)
        js-utils/mam-state-to-clj)))


(defn create
  "Creates a MAM message payload from a state object, tryte-encoded message and
  an optional side key. Returns an updated state and the payload for sending.

  Arguments:
  state - map Initialised IOTA MAM library with a provider set.
  message - string Tryte-encoded payload to be encrypted.

  Returns a map with:
  :state - map Updated state object to be used with future actions.
  :payload - string Tryte-encoded payload.
  :root - string Tryte-encoded root of the payload.
  :address - string Tryte-encoded address used as an location to attach the
                    payload."
  [state message]
  (let [js-state (js-utils/mam-state-to-js state)]
    (-> (.create js/Mam js-state message)
        js-utils/mam-msg-to-clj)))


(defn decode
  "Enables a user to decode a payload.

  Arguments
  payload - string Tryte encoded payload.
  side-key - string Tryte-encoded encryption key. `nil` value falls back to
                    default key.
  root - string Tryte-encoded string used as the address to attach the payload.

  Returns map with
  :state - object Updated state object to be used with future actions.
  :payload - string Tryte-encoded payload.
  :root - string Tryte-encoded root used as an address to attach the payload.

  NOTE: throws ugly errors like Unwind_GetIPInfo and trap! when input is
        invalid (like wrong root)"
  [payload side-key root]
  (-> (.decode js/Mam payload side-key root)
      js-utils/mam-msg-to-clj))


;;;;
;;;; Network Usage

;;; These actions require an initialised IOTA library with a provider to be
;;; passed in when calling `init`.

(defn attach
  "Attaches a payload to the tangle.

  payload - string Tryte-encoded payload to be attached to the Tangle.
  address - string Tryte-encoded string returned from the `create` function.
  depth - int Value that determines how far to go for tip selection.
  min-weight-magnitude - int Minimum weight magnitude.

  Returns a core.async channel that receives transaction objects that have been
  attached to the network."
  ([payload address]
   (attach payload address 6))
  ([payload address depth]
   (attach payload address depth 14))
  ([payload address depth min-weight-magnitude]
   (let [ch (chan)]
     (.then (.attach js/Mam payload address depth min-weight-magnitude)
            #(go (>! ch (js-utils/js->cljkk %))))
     (log/info "Performing Proof of Work...")
     ch)))


(defn fetch
  "Fetches the stream sequentially from a known `root` and optional `side-key`.
  This call can be used in two ways: Without a callback will cause the function
  to read the entire stream before returning. With a callback the application
  will return data through the callback and finally the `next-root` when
  finished.

  Arguments:
  root - string Tryte-encoded string used as the entry point to a
                stream. NOT the address!
  mode - keyword Stream mode. Can be only: `:public`, `:private` or
                 `:restricted`. `nil` value falls back to `:public`.
  side-key - string Tryte-encoded encryption key. `nil` value falls back to
                    default key

  callback: fn Callback. `nil` value will cause the function to read the entire
                         stream before returning

  Returns map with:
  `:next-root` - string Tryte-encoded string pointing to the next root.
  `:messages` - coll of Tryte-encoded messages from the stream. NOTE: This is
                     only returned when the call is not using a callback."
  ([root mode]
   (fetch root mode nil nil))
  ([root mode side-key]
   (fetch root mode side-key nil))
  ([root mode side-key f]
   (let [ch   (chan)
         mode (if mode (name mode) "public")]
     (.then (.fetch js/Mam root mode side-key f)
            #(go (when % (>! ch (js-utils/js->cljkk %)))))
     ch)))
