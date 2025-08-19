(ns g-mcp.mail
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn run-applescript
  "Execute AppleScript and return result"
  [script]
  (try
    (let [result (shell/sh "osascript" "-e" script)]
      (if (= 0 (:exit result))
        {:success true :output (str/trim (:out result))}
        {:success false :error (:err result)}))
    (catch Exception e
      {:success false :error (.getMessage e)})))

(defn check-mail-access
  "Check if Mail.app is accessible"
  []
  (let [script "tell application \"Mail\" to return name of account 1"]
    (run-applescript script)))

(defn get-mailboxes
  "Get list of available mailboxes"
  []
  (let [script "tell application \"Mail\"
                  set mailboxList to {}
                  repeat with acc in accounts
                    repeat with mbox in mailboxes of acc
                      set end of mailboxList to (name of mbox & \" (\" & name of acc & \")\")
                    end repeat
                  end repeat
                  return my listToString(mailboxList, \"\n\")
                end tell

                on listToString(lst, delim)
                  set AppleScript's text item delimiters to delim
                  set str to lst as string
                  set AppleScript's text item delimiters to \"\"
                  return str
                end listToString"]
    (run-applescript script)))

(defn read-emails
  "Read emails from Mail.app"
  [account-name max-results query]
  (try
    (let [search-clause (if (and query (not (str/blank? query)))
                          (str "whose subject contains \"" query "\"")
                          "")
          script (str "tell application \"Mail\"
                        set resultList to {}
                        try
                          set targetAccount to first account whose name is \"" account-name "\"
                          set acctMailboxes to every mailbox of targetAccount

                          repeat with mb in acctMailboxes
                            try
                              -- Focus on INBOX first for recent emails
                              if name of mb is \"INBOX\" then
                                set messagesList to (messages of mb " search-clause ")
                                set msgLimit to " max-results "
                                if msgLimit > (count of messagesList) then
                                  set msgLimit to (count of messagesList)
                                end if

                                -- Get the first N messages (most recent, since newest are first)
                                repeat with i from 1 to msgLimit
                                  try
                                    set currentMsg to item i of messagesList
                                    set msgSubject to subject of currentMsg
                                    set msgSender to sender of currentMsg
                                    set msgDate to (date sent of currentMsg) as string
                                    set msgID to id of currentMsg
                                    set msgRead to read status of currentMsg
                                    set mailboxName to name of mb

                                    try
                                      set msgContent to content of currentMsg
                                      if length of msgContent > 500 then
                                        set msgContent to (text 1 thru 500 of msgContent) & \"...\"
                                      end if
                                    on error
                                      set msgContent to \"[Content not available]\"
                                    end try

                                    set emailInfo to \"ID: \" & msgID & \"\n\" & \"Subject: \" & msgSubject & \"\n\" & \"From: \" & msgSender & \"\n\" & \"Date: \" & msgDate & \"\n\" & \"Read: \" & msgRead & \"\n\" & \"Mailbox: \" & mailboxName & \"\n\" & \"Content: \" & msgContent & \"\n---MESSAGE_SEPARATOR---\n\"
                                    set end of resultList to emailInfo
                                  on error
                                    -- Skip problematic messages
                                  end try
                                end repeat
                                exit repeat -- Found INBOX, no need to check other mailboxes
                              end if
                            on error
                              -- Skip problematic mailboxes
                            end try
                          end repeat
                        on error errMsg
                          return \"Error: \" & errMsg
                        end try

                        return my listToString(resultList, \"\")
                      end tell

                      on listToString(lst, delim)
                        set AppleScript's text item delimiters to delim
                        set str to lst as string
                        set AppleScript's text item delimiters to \"\"
                        return str
                      end listToString")
          result (run-applescript script)]
      (if (:success result)
        (let [output (:output result)
              messages (if (str/blank? output)
                         []
                         (->> (str/split output #"---MESSAGE_SEPARATOR---")
                              (map str/trim)
                              (filter #(not (str/blank? %)))
                              (map (fn [msg-text]
                                     (let [lines (str/split msg-text #"\n")
                                           parse-line (fn [prefix line]
                                                        (when (str/starts-with? line prefix)
                                                          (str/trim (subs line (count prefix)))))]
                                       {:id (or (some #(parse-line "ID: " %) lines) "")
                                        :subject (or (some #(parse-line "Subject: " %) lines) "")
                                        :from (or (some #(parse-line "From: " %) lines) "")
                                        :date (or (some #(parse-line "Date: " %) lines) "")
                                        :read (= "true" (or (some #(parse-line "Read: " %) lines) "false"))
                                        :mailbox (or (some #(parse-line "Mailbox: " %) lines) "")
                                        :content (or (some #(parse-line "Content: " %) lines) "")})))))]
          {:success true
           :total-messages (count messages)
           :account account-name
           :messages messages})
        {:success false
         :error (:error result)
         :message "Failed to read emails from Mail.app"}))
    (catch Exception e
      {:success false
       :error (.getMessage e)
       :message "Failed to execute AppleScript"})))

(defn create-draft
  "Create an email draft in Mail.app"
  [account to subject body cc bcc]
  (try
    (let [cc-clause (if (and cc (not (str/blank? cc)))
                      (str "set Cc recipients to \"" cc "\"")
                      "")
          bcc-clause (if (and bcc (not (str/blank? bcc)))
                       (str "set Bcc recipients to \"" bcc "\"")
                       "")
          account-script (if (and account (not (str/blank? account)))
                           (str "tell newMessage to set sender to \"" account "\"")
                           "")
          script (str "tell application \"Mail\"
                        set newMessage to make new outgoing message with properties {subject:\"" subject "\", content:\"" body "\", visible:false}
                        tell newMessage
                          make new to recipient at end of to recipients with properties {address:\"" to "\"}
                          " cc-clause "
                          " bcc-clause "
                        end tell
                        " account-script "

                        -- Save the message ID before closing
                        set msgId to id of newMessage

                        -- Save and close the draft
                        save newMessage
                        close newMessage

                        return msgId
                      end tell")
          result (run-applescript script)]
      (if (:success result)
        {:success true
         :draft-id (:output result)
         :account account
         :message "Draft created successfully in Mail.app"}
        {:success false
         :error (:error result)
         :message "Failed to create draft in Mail.app"}))
    (catch Exception e
      {:success false
       :error (.getMessage e)
       :message "Failed to create draft"})))

(defn list-accounts
  "List all mail accounts configured in Mail.app"
  []
  (let [script "tell application \"Mail\"
                  set accountList to {}
                  repeat with acc in accounts
                    set end of accountList to name of acc
                  end repeat
                  return my listToString(accountList, \"\n\")
                end tell

                on listToString(lst, delim)
                  set AppleScript's text item delimiters to delim
                  set str to lst as string
                  set AppleScript's text item delimiters to \"\"
                  return str
                end listToString"
        result (run-applescript script)]
    (if (:success result)
      {:success true
       :accounts (if (str/blank? (:output result))
                   []
                   (str/split (:output result) #"\n"))}
      {:success false
       :error (:error result)})))
