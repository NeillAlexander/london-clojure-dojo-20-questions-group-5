(ns questions.core
  (:require [clj-http.client :as client]
            ))

(def url "http://172.6.1.227:8080/20-questions/latest")

(defn get-data [url]
  (read-string (:body (client/get url))))

(defn ask-question [data]
  (:question data))

(defn respond-to-question [data answer]
  (data answer)) 

(defn is-question? [answer]
  (map? answer))

(defn is-person? [answer]
  (not (is-question? answer)))

(defn make-question [question new-options]
  (merge {:question question} new-options))

(defn suggest-new-question [data answer question alt-key alt]
  (let [previous (data answer)]
    (assoc data answer (make-question question {alt-key alt answer previous}))))

(comment
  (defn play-game [{:keys [ question yes no]} ]
    (println question )
    (let [input (keyword (read-line))]
      (if (= :yes input)) )))
