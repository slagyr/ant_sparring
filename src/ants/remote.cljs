(ns ants.remote
  (:require [ants.log :as log]
            [taoensso.sente :as sente]))

(defonce channel-socket (atom :uninitialized))
(defonce receive-channel (atom :uninitialized))
(defonce send! (atom :uninitialized))
(defonce channel-state (atom :uninitialized))

(defn assert-connection []
  (when (= :uninitialized @channel-state)
    (throw (js/Error. "attempt to used uninitialized websocket"))))

(defn default-success [result]
  (log/info "success: " result))

(defn default-error [result]
  (log/info "error: " result))

(def default-options
  {:timeout    5000
   :on-success default-success
   :on-error   default-error})

(defn dispatch-result [on-success on-error response]
  (log/debug "remote dispatching response: " response)
  (try
    (cond
      (= :ok (:status response)) (on-success response)
      (= :error (:status response)) (on-error response)
      :else (throw (ex-info "Remote Error:" {:response response})))
    (finally
      (log/debug "remote finished response dispatch"))))

(defn call! [remote-fn data & option-args]
  (assert-connection)
  (log/debug "remote starting: remote-fn, data: " remote-fn data)
  (let [options (apply hash-map option-args)
        options (merge default-options options)
        {:keys [on-error on-success timeout]} options]
    (@send! [remote-fn data] timeout (partial dispatch-result on-success on-error))))

(def on-connect-actions (atom []))

(defn on-connect [action]
  (if (:open? @channel-state)
    (action)
    (swap! on-connect-actions conj action)))


; ----------------- handlers ----------------------------

(defmulti event-msg-handler :id)                            ; Dispatch on event-id

(defn event-msg-handler* [{:as ev-msg :keys [id ?data event]}]
  (log/debug "incomming event: " event)
  (event-msg-handler ev-msg))

(defmethod event-msg-handler :default [{:as ev-msg :keys [event]}]
  (log/warn "Unhandled event: " event))

(defmethod event-msg-handler :chsk/state [{:as ev-msg :keys [?data]}]
  (if (:first-open? ?data)
    (let [actions @on-connect-actions]
      (reset! on-connect-actions [])
      (doseq [action actions]
        (action)))
    nil))

(defmethod event-msg-handler :chsk/recv [{:as ev-msg :keys [?data]}]
  (log/debug "Push event from server: " ?data))


(defmethod event-msg-handler :chsk/handshake [{:as ev-msg :keys [?data]}]
  (log/debug "handshake: " ?data))

; ----------------- end handlers ------------------------

(def router (atom nil))

(defn stop-router! []
  (when-let [stop-f @router]
    (stop-f)))

(defn start-router! []
  (log/info "starting client remote router")
  (stop-router!)
  (reset! router (sente/start-chsk-router! @receive-channel event-msg-handler*)))

(defn csrf-token []
  (assert-connection)
  (:csrf-token @@channel-state :MISSING!))

(defn connect! []
  (if (= :uninitialized @receive-channel)
    (let [{:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket! "/chsk" {:type :auto})]
      (reset! channel-socket chsk)
      (reset! receive-channel ch-recv)
      (reset! send! send-fn)
      (reset! channel-state state)
      (start-router!))
    (throw (ex-info "Attempt to RE-open websocket!" {}))))