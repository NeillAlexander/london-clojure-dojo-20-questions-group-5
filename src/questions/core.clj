(ns questions.core
  (:require [clj-http.client :as client]
            [swank.swank :as swank]
            [clojure.string :as str]))

;; For the online version we did at the dojo
;; See https://github.com/ppotter/questions-server
;; The server didn't run when I cloned it locally so I've only done the work
;; to make the game work locally.
(def url "http://172.6.1.227:8080/20-questions/latest")

(defn get-data [url]
  (read-string (:body (client/get url))))

;; This will gradually grow through the lifetime of the game
(def starting-data {:question "Are you alive?", :yes "Trevor McDonald", :no "Elizabeth I"})

(def transform-response {"y", :yes, "yes" :yes, "n" :no, "no" :no})

(def validate-response (set (keys transform-response)))

(defn make-game-data [questions]
  {:questions questions, :responses []})

(defn is-question? [answer]
  (map? answer))

(defn is-valid-game [data]
  (and (map? data) (:questions data)))

(defn prompt!
  "If supplied, valid is passed to check the input to make sure it is valid."
  ([s]
     (prompt! s str/blank?))
  ([s validate transform]
     (println s)
     (flush)
     (let [input (str/trim (read-line))]
       (if (not (validate input))
         (recur s validate transform)
         (transform input)))))

(defn prompt-response! [s]
  (prompt! s validate-response transform-response))

(defn next-question [{:keys [questions responses]}]
  (if (seq responses)
    (:question (get-in questions responses))
    (:question questions)))

(defn current-response [{:keys [questions responses]}]
  (get-in questions responses))

(defn add-response [data response]
  (update-in data [:responses] conj response))

(defn wipe-responses [data]
  (assoc-in data [:responses] []))

(defn ask-question
  "Prints the question and prompts for a response, returning :yes or :no"
  [data]
  (prompt-response! (next-question data)))

(defn guess [data]
  (let [response (prompt-response! (str "Are you " (current-response data) "?"))]
    (if (= :yes response)
      (println "Yay! I win!!")
      data)))

(defn play
  [data]
  (let [data (add-response data (ask-question data))]
    (if (is-question? (current-response data))
      (recur data)
      (guess data))))

(defn -main [& [swank? :as args]]
  (if swank? (swank/start-repl 4005))
  (loop [data (make-game-data starting-data)]
    (if (is-valid-game data)
      (recur (wipe-responses (play data)))
      (System/exit 1))))
