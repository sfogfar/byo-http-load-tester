(ns http-load-tester.core
  (:gen-class)
  (:require
   [clj-http.client :as client]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))

(s/def ::url-string (s/and string?
                           #(try
                              (java.net.URL. %)  ;; Try to create a URL object
                              (not (str/blank? %))
                              (catch Exception _ false))))

(defn get-request
  [url]
  (if (s/valid? ::url-string url)
    (try
      (client/get url)
      (catch Exception e
        {:error (str "Request failed: " (.getMessage e))}))
    {:error (str "Invalid URL: " (s/explain-str ::url-string url))}))

(defn -main
  [& args]
  (let [[url] args]
    (try
      (cond
        (= 1 (count args)) (println (str "Response code: " (:status (get-request url))))
        :else (throw (Exception. "Invalid arguments")))
      (catch Exception e
        (println "Error:" (.getMessage e))
        (System/exit 1)))))

(comment
  (def example-url  "http://localhost:8000")
  (-main example-url)

  (s/valid? ::url-string example-url)
  (s/valid? ::url-string "http//localhost:8000")

  ;
  )
