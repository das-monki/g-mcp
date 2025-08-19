(ns g-mcp.gmail
  (:require [g-mcp.auth :as auth]
            [clojure.string :as str])
  (:import [com.google.api.services.gmail.model Message]
           [com.google.api.client.util Base64]))

(defn- get-header-value [headers header-name]
  (->> headers
       (filter #(= (.getName %) header-name))
       first
       .getValue))

(defn- decode-base64-url [encoded-str]
  (try
    (when encoded-str
      (String. (Base64/decodeBase64 encoded-str) "UTF-8"))
    (catch Exception _
      encoded-str)))

(defn- extract-body-from-part [part]
  (let [body (.getBody part)
        data (.getData body)]
    (when data
      (decode-base64-url data))))

(defn- extract-text-body [parts]
  (when parts
    (let [text-part (first (filter #(= "text/plain" (.getMimeType %)) parts))
          html-part (first (filter #(= "text/html" (.getMimeType %)) parts))]
      (or (when text-part (extract-body-from-part text-part))
          (when html-part (extract-body-from-part html-part))
          ""))))

(defn- parse-message [message gmail-service]
  (try
    (let [full-message (.execute (.get (.messages gmail-service) "me" (.getId message)))
          payload (.getPayload full-message)
          headers (.getHeaders payload)
          parts (.getParts payload)]
      {:id (.getId full-message)
       :thread-id (.getThreadId full-message)
       :snippet (.getSnippet full-message)
       :from (get-header-value headers "From")
       :to (get-header-value headers "To")
       :subject (get-header-value headers "Subject")
       :date (get-header-value headers "Date")
       :body (or (extract-text-body parts)
                 (extract-body-from-part payload)
                 (.getSnippet full-message))
       :labels (.getLabelIds full-message)})
    (catch Exception e
      {:id (.getId message)
       :error (.getMessage e)
       :snippet (.getSnippet message)})))

(defn read-emails
  "Read emails from Gmail for a specific domain user"
  [domain max-results query]
  (try
    (let [gmail-service (auth/get-gmail-service)
          request (-> gmail-service
                      .messages
                      (.list "me"))
          _ (when (and query (not (str/blank? query)))
              (.setQ request query))
          _ (.setMaxResults request (long max-results))
          response (.execute request)
          messages (.getMessages response)]

      (if messages
        {:success true
         :total-messages (.size messages)
         :domain domain
         :messages (map #(parse-message % gmail-service) messages)}
        {:success true
         :total-messages 0
         :domain domain
         :messages []
         :message "No messages found"}))
    (catch Exception e
      {:success false
       :error (.getMessage e)
       :domain domain
       :message (str "Failed to read emails for domain: " domain)})))

(defn create-draft
  "Create an email draft in Gmail"
  [domain to subject body cc bcc]
  (try
    (let [gmail-service (auth/get-gmail-service)
          message (doto (Message.)
                    (.setRaw (-> (StringBuilder.)
                                 (.append (str "To: " to "\r\n"))
                                 (.append (when (and cc (not (str/blank? cc)))
                                            (str "Cc: " cc "\r\n")))
                                 (.append (when (and bcc (not (str/blank? bcc)))
                                            (str "Bcc: " bcc "\r\n")))
                                 (.append (str "Subject: " subject "\r\n"))
                                 (.append "\r\n")
                                 (.append body)
                                 .toString
                                 .getBytes
                                 Base64/encodeBase64URLSafeString)))
          draft (doto (com.google.api.services.gmail.model.Draft.)
                  (.setMessage message))
          request (-> gmail-service
                      .drafts
                      (.create "me" draft))
          response (.execute request)]

      {:success true
       :domain domain
       :draft-id (.getId response)
       :message-id (-> response .getMessage .getId)
       :message "Draft created successfully"})
    (catch Exception e
      {:success false
       :error (.getMessage e)
       :domain domain
       :message (str "Failed to create draft for domain: " domain)})))
