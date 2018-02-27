(ns cljs-iota-mam.run-tests
  (:require [cljs-iota-mam.core-test]
            [cljs.test :refer-macros [run-tests]]))


(defn run-all-tests []
  (enable-console-print!)
  (.clear js/console)

  ;; Wait for wasm compile to finish before running tests...
  (js/setTimeout
   #(run-tests 'cljs-iota-mam.core-test)
   1000))
