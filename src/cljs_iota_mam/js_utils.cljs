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
(def snake-case (safe-case kebab/->snake_case))


(def js->cljk #(js->clj % :keywordize-keys true))


(defn keywordize-mode
  "Keywordizes the MAM mode (`:restricted`, `:public`, or `:private`) in a MAM
  clj message."
  [mam-msg]
  (update-in mam-msg [:state :channel :mode] keyword))


(defn stringify-mode
  "Stringifies the MAM mode (`:restricted`, `:public`, or `:private`) in a MAM
  clj message."
  [mam-msg]
  (update-in mam-msg [:state :channel :mode] name))


(def js->cljkk
  "From JavaScript to Clojure with kekab-cased keywords."
  (comp (partial transform-keys kebab-case) js->cljk))


(def cljkk->js
  "From Clojure to JavaScript object with snake_case keys."
  (comp clj->js (partial transform-keys snake-case)))


(defn mam-state-to-clj [state]
  (-> state
      js->cljkk
      keywordize-mode))


(defn mam-state-to-js [state]
  (if (map? state)
    (-> state
        stringify-mode
        cljkk->js)
    state))
