(ns echo-server.core
  (:gen-class)
  (:require [ring.adapter.jetty :as jetty]
            [reitit.ring :as ring]
            [clojure.spec.alpha :as s]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [reitit.ring.coercion :as coercion]
            [ring.middleware.params :as params]
            [reitit.coercion.spec]
            [ring.middleware.reload :refer [wrap-reload]]))

(s/def ::name int?)

(defn handler [{{{:keys [name]} :path} :parameters :as params}]
  (println "Headers = " (:headers params))
  {:status 200 :body (str "hello " (dec name))})

(def ping-route
  {:summary ""
   :parameters {:path (s/keys :req-un [::name])}
   :responses {200 {:body string?}}
   :handler handler})

(def router
  (ring/ring-handler
   (ring/router
    ["" {:coercion reitit.coercion.spec/coercion}
     ["/ping/:name" {:get ping-route}]]
    {:data {:muuntaja m/instance
            :middleware [params/wrap-params
                         muuntaja/format-middleware
                         coercion/coerce-exceptions-middleware
                         coercion/coerce-request-middleware
                         coercion/coerce-response-middleware]}})
   (ring/create-default-handler)))

(def dev-router
  (wrap-reload router))

(defn start [used-router]
  (jetty/run-jetty used-router {:port 3000, :join? false})
  (println "server running in port 3000"))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (let [used-router (if (= (System/getenv "ENV") "dev")
                      #'dev-router
                      #'router)]
    (println "Starting")
    (start used-router)))
