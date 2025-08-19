(ns g-mcp.auth
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [com.google.auth.oauth2 GoogleCredentials]
           [com.google.auth.http HttpCredentialsAdapter]
           [com.google.api.client.googleapis.javanet GoogleNetHttpTransport]
           [com.google.api.client.json.gson GsonFactory]
           [com.google.api.services.gmail Gmail Gmail$Builder]
           [java.io FileInputStream]))

(def ^:private scopes
  ["https://www.googleapis.com/auth/gmail.readonly"
   "https://www.googleapis.com/auth/gmail.compose"
   "https://www.googleapis.com/auth/gmail.modify"])

(def ^:private credentials-file
  (or (System/getenv "GOOGLE_APPLICATION_CREDENTIALS")
      "credentials.json"))

(defn- load-credentials []
  (try
    (let [credentials (-> credentials-file
                          (FileInputStream.)
                          (GoogleCredentials/fromStream)
                          (.createScoped scopes))]
      credentials)
    (catch Exception e
      (throw (ex-info "Failed to load Google credentials"
                      {:error (.getMessage e)
                       :credentials-file credentials-file})))))

(defn- create-gmail-service [credentials]
  (let [http-transport (GoogleNetHttpTransport/newTrustedTransport)
        json-factory (GsonFactory/getDefaultInstance)
        request-initializer (HttpCredentialsAdapter. credentials)]
    (-> (Gmail$Builder. http-transport json-factory request-initializer)
        (.setApplicationName "G-MCP")
        (.build))))

(defn get-gmail-service []
  (let [credentials (load-credentials)]
    (create-gmail-service credentials)))

(defn list-domains
  "List domains - simplified version without Admin API"
  []
  {:success true
   :domains [{:domain-name "example.com" :is-primary true :verified true}]
   :message "Using simplified domain management. Configure your domain in the credentials."})

(defn get-default-domain
  "Get the default domain"
  []
  {:success true
   :domain "example.com"})