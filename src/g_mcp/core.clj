(ns g-mcp.core
  (:require [modex.mcp.server :as server]
            [modex.mcp.tools :as tools]
            [g-mcp.gmail :as gmail]
            [g-mcp.auth :as auth])
  (:gen-class))

(def gmail-tools
  (tools/tools
   (read-emails
    "Read emails from a Google Workspace domain"
    [{:keys [domain max-results query]
      :type {domain :string max-results :number query :string}
      :or {max-results 10 query ""}}]
    (gmail/read-emails domain max-results query))

   (create-draft
    "Create an email draft in Google Workspace"
    [{:keys [domain to subject body cc bcc]
      :type {domain :string to :string subject :string body :string cc :string bcc :string}
      :or {cc "" bcc ""}}]
    (gmail/create-draft domain to subject body cc bcc))

   (list-domains
    "List available domains for the authenticated Google Workspace"
    [{}]
    (auth/list-domains))))

(def g-mcp-server
  (server/->server
   {:name "G-MCP Google Workspace Server"
    :version "0.1.0"
    :initialize (fn [_]
                  (println "Initializing G-MCP Google Workspace Server")
                  "G-MCP initialized successfully")
    :tools gmail-tools
    :prompts nil
    :resources nil}))

(defn -main
  "Start the G-MCP server"
  [& args]
  (println "Starting G-MCP Google Workspace MCP Server...")
  (server/start-server! g-mcp-server))
