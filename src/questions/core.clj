(ns questions.core
  (:require [clj-http.client :as client]))

(def url "http://172.6.1.227:8080/20-questions/latest")

(defn get-data [url]
  (read-string (:body (client/get url))))

(defn play-game [{:keys [ question yes no]} ]
  (println question ))
