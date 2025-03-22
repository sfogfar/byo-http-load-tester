(ns http-load-tester.core
  (:gen-class)
  (:require
   [clj-http.client :as client]
   [clojure.core.async :refer [<! <!! >! >!! chan go-loop]]
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
      (let [response (client/get url {:as :stream
                                      :retry-handler (fn [& _] false)
                                      :socket-timeout 5000
                                      :connection-timeout 3000})]
        ;; Close the stream and discard the body
        (when-let [body (:body response)]
          (.close body))
        (dissoc response :body))
      (catch Exception e
        {:error (str "Request failed: " (.getMessage e))}))
    {:error (str "Invalid URL: " (s/explain-str ::url-string url))}))

(defn make-requests
  [{:keys [url request-count concurrent-requests]}]
  (let [request-channel (chan request-count)
        response-channel (chan request-count)
        responses (atom [])]

    (dotimes [i request-count]
      (>!! request-channel i))

    (dotimes [_ concurrent-requests]
      (go-loop []
        (when-let [_ (<! request-channel)]
          (>! response-channel (get-request url)))
        (recur)))

    (dotimes [_ request-count]
      (swap! responses conj (<!! response-channel)))

    @responses))

(defn parse-args
  [& args]
  (try
    (cond
      ;; -u <url>
      (and (= "-u" (first args))
           (= 2 (count args)))
      {:url (second args)
       :request-count 1
       :concurrent-requests 1}

      ;; -u <url> -n <request-count>
      (and (= "-u" (first args))
           (= "-n" (nth args 2))
           (= 4 (count args)))
      {:url (second args)
       :request-count (Integer/parseInt (last args))
       :concurrent-requests 1}

      ;; -u <url> -n <request-count> -c <concurrent-requests>
      (and (= "-u" (first args))
           (= "-n" (nth args 2))
           (= "-c" (nth args 4))
           (= 6 (count args)))
      {:url (second args)
       :request-count (Integer/parseInt (nth args 3))
       :concurrent-requests (Integer/parseInt (last args))}

      :else
      {:error "Usage: program -u <url> [-n <request-count>]"})
    (catch NumberFormatException _e
      {:error "Error: COUNT must be a number"})))

(defn summarise
  [responses]
  (let [network-errors (filter #(:error %) responses)
        http-errors (filter #(when-let [status (:status %)]
                               (not (<= 200 status 299))) responses)
        failure-count (+ (count network-errors) (count http-errors))
        success-count (- (count responses) failure-count)]
    {:failure-count failure-count
     :success-count success-count}))

(defn -main
  [& args]
  (try
    (let [parsed-args (apply parse-args args)]
      (if (:error parsed-args)
        (do
          (println (:error parsed-args))
          (System/exit 1))
        (let [responses (make-requests parsed-args)
              summary (summarise responses)]
          (println (str "Successes: " (:success-count summary)))
          (println (str "Failures: " (:failure-count summary))))))
    (catch Exception e
      (println "Error:" (.getMessage e))
      (System/exit 1))))

(comment
  (def example-url  "http://localhost:8000/large_file.bin")
  (-main "-u" example-url "-n" "3")
  (-main "-u" example-url "-n" "100" "-c" "10")
  (-main "-u" example-url "-n" "500" "-c" "50")
  (-main "-u" example-url "-n" "1000" "-c" "100")

  (s/valid? ::url-string example-url)
  (s/valid? ::url-string (str/replace example-url ":" ""))

  ;
  )
