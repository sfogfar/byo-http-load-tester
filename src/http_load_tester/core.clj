(ns http-load-tester.core
  (:gen-class)
  (:require
   [clj-http.client :as client]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))

(s/def ::url-string
  (s/and string?
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

(defn make-requests
  [{:keys [url request-count]}]
  (map (fn [_] (get-request url)) (range request-count)))

(defn parse-args
  [& args]
  (try
    (cond
      ;; -u <url>
      (and (= (first args) "-u") (= 2 (count args)))
      {:url (second args) :request-count 1}

      ;; -u <url> -n <request-count>
      (and (= (first args) "-u") (= (nth args 2) "-n") (= 4 (count args)))
      {:url (second args) :request-count (Integer/parseInt (last args))}

      :else
      {:error "Usage: program -u <url> [-n <request-count>]"})
    (catch NumberFormatException _e
      {:error "Error: COUNT must be a number"})))

(defn -main
  [& args]
  (try
    (let [parsed-args (apply parse-args args)]
      (if (:error parsed-args)
        (do
          (println (:error parsed-args))
          (System/exit 1))
        (let [responses (make-requests parsed-args)]
          (doseq [response responses]
            (if (:status response)
              (println (str "Response code: " (:status response)))
              (println (str "Error! " (:error response))))))))
    (catch Exception e
      (println "Error:" (.getMessage e))
      (System/exit 1))))

(comment
  (def example-url  "http://localhost:8000/hello.txt")
  (-main "-u" example-url)
  (-main "-u" example-url "-n" "3")

  (s/valid? ::url-string example-url)
  (s/valid? ::url-string (str/replace example-url ":" ""))

  ;
  )
