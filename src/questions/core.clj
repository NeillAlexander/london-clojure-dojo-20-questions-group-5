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
(def starting-data {:question "Are you a news-reader?", :yes "Trevor McDonald", :no "Elizabeth I"})

;; Use this to change the response to :yes or :no
(def transform-response {"y", :yes, "yes" :yes, "n" :no, "no" :no})

;; Use this to make sure user enters y, yes, n or no
(def validate-response (set (keys transform-response)))

;; Create the data structure. We embed the vector of responses so that we can navigate the tree.
(defn make-game-data [questions]
  {:questions questions, :responses []})

;; A question is a map like: {:question "Are you a x?" :yes "Yes answer" :no "No answer"
(defn is-question? [answer]
  (map? answer))

;; Checks that data matches the expected game structure
(defn is-valid-game [data]
  (and (map? data) (:questions data)))

;; Prompt user for input, validate with a function, and transform.
(defn prompt!
  "If supplied, valid is passed to check the input to make sure it is valid."
  ([s]
     (prompt! s #(not (str/blank? %)) identity))
  ([s validate transform]
     (println)
     (println s)
     (flush)
     (let [input (str/trim (read-line))]
       (if (not (validate input))
         (recur s validate transform)
         (transform input)))))

;; Helper method for the most common case where we prompt for yes / no
(defn prompt-response! [s]
  (prompt! s validate-response transform-response))

;; Navigate the game data to pull out the next question based on the responses.
(defn next-question [{:keys [questions responses]}]
  (if (seq responses)
    (:question (get-in questions responses))
    (:question questions)))

;; Based on the responses in the game data, what is the current value?
(defn current-response-value [{:keys [questions responses]}]
  (get-in questions responses))

;; What was the users last response?
(defn last-response-key [{:keys [responses]}]
  (last responses))

;; Add the response to the game data
(defn add-response [data response]
  (update-in data [:responses] conj response))

;; Clear the responses from the game data, ready for a new game
(defn wipe-responses [data]
  (assoc-in data [:responses] []))

;; Ask the question and return the response
(defn ask-question
  "Prints the question and prompts for a response, returning :yes or :no"
  [data]
  (prompt-response! (next-question data)))

;; Update the game data
(defn replace-guess-with-question [data question]
  (assoc-in data (apply vector :questions (:responses data)) question))

;; Ask for a new question to add to the game data
(defn improve-questions [data]
  (let [last-guess (current-response-value data)
        new-question (prompt! (str "What yes/no question could I ask that would distinguish you from " last-guess "?"))
        response-key (prompt-response! (str "Would the answer be yes or no for " last-guess "?"))
        other-answer (prompt! (str "What would be the " (if (= :yes response-key) "no" "yes") " answer?"))]    
    (replace-guess-with-question data {:question new-question, response-key last-guess, (if (= :yes response-key) :no :yes) other-answer})))

;; Display the guess and either declare victory or prompt to improve the questions
(defn guess [data]
  (let [response (prompt-response! (str "Are you " (current-response-value data) "?"))]
    (if (= :yes response)
      (do
        (println "Yay! I win!!")
        (if (= :yes (prompt-response! "Play again?"))
          data))
      (improve-questions data))))

;; The game loop
(defn play
  [data]
  (let [data (add-response data (ask-question data))]
    (if (is-question? (current-response-value data))
      (recur data)
      (guess data))))

;; Entry point for running
(defn -main [& [swank? :as args]]
  (if swank? (swank/start-repl 4005))
  (loop [data (make-game-data starting-data)]
    (if (is-valid-game data)
      (recur (wipe-responses (play data)))
      (System/exit 1))))
