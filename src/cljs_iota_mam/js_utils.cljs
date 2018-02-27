(ns cljs-iota-mam.js-utils
  "Utilities for interacting with JavaScript.

  Copied from cljs-web3 by district0x."
  (:require [camel-snake-kebab.core :as kebab :include-macros true]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [clojure.string :as string]))


(defn safe-case [case-fn]
  (fn [x]
    (cond-> (subs (name x) 1)
      true         case-fn
      true         (->> (str (first (name x))))
      (keyword? x) keyword)))


(def camel-case (safe-case kebab/->camelCase))
(def kebab-case (safe-case kebab/->kebab-case))


(def js->cljk #(js->clj % :keywordize-keys true))


(def js->cljkk
  "From JavaScript to Clojure with kekab-cased keywords."
  (comp (partial transform-keys kebab-case) js->cljk))


(def cljkk->js
  "From Clojure to JavaScript object with camelCase keys."
  (comp clj->js (partial transform-keys camel-case)))


(defn keywordize-mode
  "Keywordizes the MAM mode (`:restricted`, `:public`, or `:private`) in a MAM
  clj message."
  [mam-msg]
  (update-in mam-msg [:state :channel :mode] keyword))
