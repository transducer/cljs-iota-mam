(ns cljs-iota-mam.core-test
  "Unit tests that are running against a local or public node.

  Start local node via instructions in
  https://github.com/schierlm/private-iota-testnet"
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-iota-mam.core :as iota-mam]
            [cljs-iota-mam.test-utils :refer [contains-keys?
                                              test-within
                                              test-async]]
            [cljs-iota.core :as iota]
            [cljs.core.async :as async :refer [<!]]
            [cljs.test :refer-macros [async deftest is testing use-fixtures ]]
            [clojure.string :as string]
            [taoensso.timbre :as log]))


(defn gen-seed
  "Insecure way to generate a seed."
  []
  (let [seed-length             81
        int-code-A-till-bracket #(+ (rand-int 27) 65)
        int-code-for-bracket?   #(= % 91)
        null-tryte              "9"]
    (->> (repeatedly int-code-A-till-bracket)
         (take seed-length)
         (map #(if-not (int-code-for-bracket? %) (char %) null-tryte))
         (apply str))))

(def provider       "http://localhost:14700") ;; For testnet
                                              ;; use "https://testnet140.tangle.works"

(def seed           (gen-seed))               ;; We need a new seed every run in MAM
(def security-level 2)
(def iota           (iota/create-iota provider))
(def mam            (iota-mam/init iota seed security-level))


;;; We reuse the MAM message created in the first test in the next!
(def created-mam-msg (atom nil))


;;;;
;;;; Off-Tangle tests (Basic Usage)

(deftest mam-basic-usage-test
  (let [test-state (atom {:mam-msg nil})]

    (testing "MAM object properly initialized"
      (is (contains-keys? (js->clj mam :keywordize-keys true) :subscribed :channel :seed)))

    (testing "Should be able to change mode"
      (is (= "IREALLYENJOYPOTATORELATEDPRODUCTS"
             (-> mam
                 (iota-mam/change-mode :private "IREALLYENJOYPOTATORELATEDPRODUCTS")
                 :channel
                 :side-key))))

    (testing "Should be able to create a message"
      (let [mam-msg (iota-mam/create mam "HALLO")]

        (reset! created-mam-msg mam-msg)
        (swap! test-state assoc :mam-msg mam-msg)

        (is (contains-keys? mam-msg :state :payload :root :address))))

    (testing "Should be able to decode an MAM payload"
      (let [{{:keys [root payload]} :mam-msg} @test-state]
        (is (= "HALLO"
               (-> payload
                   (iota-mam/decode "IREALLYENJOYPOTATORELATEDPRODUCTS" root)
                   :payload)))))))


;;;;
;;;; On-Tangle tests (Network Usage))

(deftest attach-test
  (testing "Should be able to attach a payload to the Tangle"
    (let [depth                     6
          mwm                       3
          {:keys [address payload]} @created-mam-msg]
      (test-async
       (test-within 20000 ;; PoW takes work
                    (go
                      (let [transactions (<! (iota-mam/attach payload address depth mwm))]
                        (is (= 3 (count transactions)))
                        (is (contains-keys? (first transactions)
                                            :address
                                            :last-index
                                            :hash
                                            :attachment-timestamp
                                            :value
                                            :bundle
                                            :trunk-transaction
                                            :branch-transaction
                                            :signature-message-fragment
                                            :current-index
                                            :attachment-timestamp-upper-bound
                                            :tag
                                            :obsolete-tag
                                            :timestamp
                                            :nonce
                                            :attachment-timestamp-lower-bound)))))))))


(deftest fetch-without-callback-test
  (testing "Should be able to fetch payloads from the Tangle"
    (let [{{{:keys [side-key mode]} :channel} :state root :root} @created-mam-msg]
      (test-async
       (test-within 20000
                    (go
                      (let [{:keys [next-root messages]} (<! (iota-mam/fetch root mode side-key))]
                        (is (vector? messages))
                        (is (string? next-root))
                        (is (= "HALLO" (first messages))))))))))


(deftest fetch-with-callback-test
  (testing "Should be able to fetch payloads from the Tangle via a callback"
    (let [{{{:keys [side-key mode]} :channel} :state} @created-mam-msg
          {:keys [root payload address]}              @created-mam-msg]
      (is ((complement nil?)
           (iota-mam/fetch root
                           mode
                           "IREALLYENJOYPOTATORELATEDPRODUCTS"
                           (fn [msg]
                             (log/info "This message should be visible in log:" msg)
                             (is (= "HALLO" msg)))))))))


(deftest fetch-with-callback-failure-test
  (testing "Should be able to fetch payloads from the Tangle"
    (let [{{{:keys [side-key mode]} :channel} :state} @created-mam-msg
          {:keys [root payload address]}              @created-mam-msg]
      ;; Throws trap! wasm error from deep inside that is not catchable
      (iota-mam/fetch root
                      mode
                      "WRONGSIDEKEY"
                      (fn [msg]
                        (log/info "Never comes here..." msg))))))
