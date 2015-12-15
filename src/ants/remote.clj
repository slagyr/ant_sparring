(ns ants.remote
  (:require [ants.app :as app]
            [ants.log :as log]
            [compojure.core :refer [routes GET POST]]
            [joodo.middleware.request :refer [*request*]]
            [taoensso.sente :as sente]
            [taoensso.sente.server-adapters.http-kit :refer [sente-web-server-adapter]]
            [ants.util :as util]))

(def get-handler (app/resolution [:remote :ajax-get-or-ws-handshake-fn]))
(def post-handler (app/resolution [:remote :ajax-post-fn]))
(def send-fn (app/resolution [:remote :send-fn]))
(def connected-uids (app/resolution [:remote :connected-uids]))

(defn- ^:remote log-remote-call [data]
  (log/trace (str "remote call: " (:id data) " client: " (:client-uuid data)))
  {})

(defn- ^:remote pong [_] {:pong true})

(defn- ^:remote default-remote-handler [data]
  (log/warn "Unhandled remote event:" (:event data))
  (throw (ex-info (str "Unsupported Remote Call: " (:id data)) data)))

(defn- ^:remote untagged-handler [data]
  (log/warn "Attempt to use untagged remote: " (:id data))
  (throw (ex-info (str "Unsupported Remote Call: " (:id data)) data)))

(def builtin-handlers
  {:chsk/uidport-open  #'log-remote-call
   :chsk/uidport-close #'log-remote-call
   :chsk/ws-ping       #'pong})

(defn resolve-api [key]
  (let [api (or (get builtin-handlers key)
                (let [fullname (symbol (str "ants." (namespace key)) (name key))] (util/resolve-var-or-nil fullname))
                #'default-remote-handler)]
    (if (-> api meta :remote)
      api
      #'untagged-handler)))

;(defn checked-invoke-api [handler payload]
;  (if (and (-> handler meta :session) (nil? (:_user payload)))
;    (api/error "No current user")
;    (api/invoke-api handler payload)))

; Keys in a message:
; :?data
; :id
; :?reply-fn
; :event
; :ring-req
; :client-uuid
; :ch-recv
; :send-fn`
; :connected-uids
(defn remote-handler [{:as ev-msg :keys [id ?data ?reply-fn ring-req]}]
  (log/debug "Remote event" id (pr-str ?data))
  (when-not (or (nil? ?data) (map? ?data)) (throw (ex-info "Remote data must be a map." {:?data ?data})))
  (let [handler (resolve-api id)]
    (log/debug "Handler: " handler)
    (binding [*request* ring-req]
      (let [payload (merge ?data (dissoc ev-msg :?data))
            ;payload (assoc payload :_user (auth/current-user))
            result (handler payload)]
        (log/debug "result: " result)
        (when ?reply-fn
          (?reply-fn result))))))

(defn- uid [request]
  (:session/key request))

(defn start [app]
  (log/info "Starting new remote router")
  (let [results (sente/make-channel-socket! sente-web-server-adapter {:user-id-fn uid})
        ch-chsk (:ch-recv results)
        router (sente/start-chsk-router! ch-chsk remote-handler)]
    (assoc app :remote (-> results
                           (dissoc :ch-recv)
                           (assoc :router router)))))

(defn stop [app]
  (if-let [router (get-in app [:remote :router])]
    (let [cids (:any @@connected-uids)]
      (log/info (str "Closing " (count cids) " connection(s) and stopping remote router"))
      (doseq [cid cids] (@send-fn cid [:chsk/close]))
      (router))
    (log/warn "No remote router to stop!" (:remote app)))
  (dissoc app :remote))

(def handler
  (routes
    (GET "/chsk" req (@get-handler req))
    (POST "/chsk" req (@post-handler req))))

(defn world-updated [stuff last-logs]
  (try
    ;(log/info "stuff last-logs: " stuff (pr-str last-logs))
    ;(prn "@connected-uids: " @connected-uids)
    (doseq [uid (:any @@connected-uids)]
      ;(prn "uid: " uid)
      (@send-fn uid [:ants/update [stuff last-logs]]))
    (catch Exception e (log/error e))))