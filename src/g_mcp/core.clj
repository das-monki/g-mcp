(ns g-mcp.core
  (:require [modex.mcp.server :as server]
            [modex.mcp.tools :as tools]
            [g-mcp.mail :as mail])
  (:gen-class))

(def mail-tools
  (tools/tools
   (read-emails
    "Read emails from Mail.app."
    [{:keys [account max-results query]
      :type {account :string max-results :number query :string}
      :or {max-results 10 query ""}
      :doc {account "The account to read emails from, f.e. info@example.com"
            max-results "Maximum number of emails to read"
            query "Search query to filter emails by subject (usually empty)"}}]
    [(mail/read-emails account max-results query)])

   (create-draft
    "Create an email draft in Mail.app"
    [{:keys [account to subject body cc bcc]
      :type {account :string to :string subject :string body :string cc :string bcc :string}
      :or {cc "" bcc ""}
      :doc {account "The account to create the draft email for, f.e. info@example.com"}}]
    [(mail/create-draft account to subject body cc bcc)])

   (list-accounts
    "List all mail accounts configured in Mail.app"
    [{}]
    [(mail/list-accounts)])

   (get-mailboxes
    "Get list of available mailboxes"
    [{}]
    [(mail/get-mailboxes)])

   (check-mail-access
    "Check if Mail.app is accessible"
    [{}]
    [(mail/check-mail-access)])))

(def g-mcp-server
  (server/->server
   {:name "G-MCP Local Mail Server"
    :version "0.1.0"
    :initialize (fn
                  [_]
                  (comment "Place for long-running initialization f.e. connecting to databases, etc."))
    :tools mail-tools
    :prompts nil
    :resources nil}))

(defn -main
  "Start the G-MCP server"
  [& _args]
  (server/start-server! g-mcp-server))
