(ns cljs-iota-mam.test-utils
  (:require [cljs.core.async :as async :refer [take! chan]]
            [cljs.test :refer-macros [is async]])
  (:require-macros [cljs.core.async.macros :refer [go]]))


(defn contains-keys? [m & ks]
  (every? #(contains? m %) ks))


;; NOTE: this can be used only *once* in a deftest.
;; See also: https://clojurescript.org/tools/testing#async-testing
;; Source: https://stackoverflow.com/a/30781278
(defn test-async
  "Asynchronous test awaiting ch to produce a value or close."
  [ch]
  (async done
         (take! ch (fn [_] (done)))))


;; Source: https://stackoverflow.com/a/30781278
(defn test-within
  "Asserts that ch does not close or produce a value within ms. Returns a
  channel from which the value can be taken."
  [ms ch]
  (go
    (let [t      (async/timeout ms)
          [v ch] (async/alts! [ch t])]
      (is (not= ch t)
          (str "Test should have finished within " ms "ms."))
      v)))
