(ns worker.core
  (:require
   [clj-http.client :as client]
   [worker.fhir-server :as fhir-server]
   [clojure.java.io :as io]
   [cheshire.core :as cheshire])
  (:import [org.apache.commons.io FileUtils]
           [java.io FileInputStream PipedOutputStream PipedInputStream]
           [java.util.zip ZipOutputStream ZipEntry]))

(defn apply-mapper [page {{mapper :mapper} :run}]
  (if (and mapper (fn? mapper))
    (map mapper page)
    page))

(defn mk-respond-handler [task & [cb]]
  (fn [response]
    ;; Non stream
    (if (= :stream (get-in task [:run :config :as]))
      (with-open [reader (java.io.InputStreamReader. (:body response)) ]
        (-> reader
            cheshire/parse-stream
            (get "entry")
            (->> (map #(get-in % ["resource" "subject" ])))
            cb))
      (-> response
          :body
          :entry
          (->> (map #(get-in % [:resource])))
          (apply-mapper task)
          cb))))


(defn task-run-dir [task]
  (str (get-in task [:run :destination :file])
       "/" (get-in task [:run :project :id])
       "/" (get-in task [:run :id])))

(defn task-result-dir [task]
  (str (task-run-dir task)
       "/" (get-in task [:resourceType])))

(defn task-result-filename [task]
  (let [f (if (:gzip task) "/%010d.ndjson.gz" "/%010d.ndjson")]
    (format f (get-in task [:query-params :_page]))))
(defn task-result-filename-relative [task]
  (format "%010d.ndjson" (get-in task [:query-params :_page])))

(defn clear-run-dir [task]
  (FileUtils/deleteDirectory (io/file (task-run-dir task))))
(+ 2 2)

(defn prepare-folder [task]
  (-> task
      task-result-dir
      io/file
      .mkdirs))

(defn destination [task]
  (str (task-result-dir task) (task-result-filename task)))

(defn save-as-file [task data]
  (prepare-folder task)
  (if (:zip task)
    (with-open [zipStream  (ZipOutputStream. (io/output-stream (destination task)))
                w (io/writer zipStream)]
      (let [entry (ZipEntry. (task-result-filename-relative task))]
        (.putNextEntry zipStream entry)
        (doseq [row data]
          (cheshire/generate-stream row w)
          (.write w "\n"))))

    (with-open [w (io/writer (io/output-stream (destination task)))]
      (doseq [row data]
        (cheshire/generate-stream row w)
        (.write w "\n")))))

(defn task-upload-file-name [task]
  (str (get-in task [:run :project :id])
       "/" (get-in task [:run :id])
       "/" (get-in task [:resourceType])
       (task-result-filename task)))

(defn task-upload-url [task]
  (str (get-in task [:run :project :destination :storage :url])
       "&name=" (task-upload-file-name task)))

(def u-acm (clj-http.conn-mgr/make-reusable-conn-manager
            {:threads 100
             :default-per-route 100}))

(defn upload-to-bucket [task data]
  (let [out-stream (PipedOutputStream.)
        in-stream (PipedInputStream. out-stream)]

    (future
      (with-open [zipStream  (ZipOutputStream. out-stream)
                  w (io/writer zipStream)]
        (let [entry (ZipEntry. (task-result-filename-relative task))]
          (.putNextEntry zipStream entry)
          (doseq [row data]
            (cheshire/generate-stream row w)
            (.write w "\n"))))
      (.close out-stream))

    (deref
     (future
       (client/post
        (task-upload-url task)
        {:body in-stream
         :connection-manager u-acm
         :headers (get-in task [:run  :project :destination :storage :authorization :headers])})))))

(defn savior [task data]
  (let [destination (get-in task [:run :destination])]
    (cond
      (:file destination)
      (save-as-file task data)

      (:storage destination)
      (upload-to-bucket task data))))

(defn raise [exception]
  (prn exception)
  (println "exception message is: " (.getMessage exception)))

(defn get-fhir-data [task respond]
  (client/get
   (fhir-server/task-url task)
   (fhir-server/task-params task)
   respond
   raise))

(defn -main []
  (prn "Hello"))
