(ns utils
  (:require
   [worker.core :as worker]
   [clojure.contrib.humanize :as humanize]))

(defonce t (atom {}))
(def cm (clj-http.conn-mgr/make-reusable-conn-manager {:timeout 2 :threads 3}))

(def acm (clj-http.conn-mgr/make-reusable-async-conn-manager
           {:threads 100
            :default-per-route 100}))

(defonce fs (atom []))
(defn stop-all []
  (doseq [f @fs]
    (future-cancel f)))

(defn print-report [t]
  (println "")
  (println "------------------------")
  (let [s (->> t (map (fn [[_ v]] (:start v))) sort first)
        e (->> t (map (fn [[_ v]] (:end v))) sort last)]

    (println  "Pages      : " (count t))
    (println  "Total time : " (humanize/duration (- e s) {:number-format str})))

  (let [result (->> t
                    (map (fn [[_ v]] (:d v)))
                    sort)]
    (println "Faster     : "  (humanize/duration (first result) {:number-format str}) )
    (println "Slovest    : "  (humanize/duration (last  result) {:number-format str}) ))
  (println "------------------------"))

(defn run-thread [task start end]
  (future
    (doseq [n (range start (inc end))]
      (swap! utils/t assoc-in [n :start] (System/currentTimeMillis))
      (let [task (-> task
                     (assoc-in  [:query-params :_page] n)
                     (assoc-in  [:query-params :_count] (:count task)))]
        (worker/get-fhir-data
         task
         (worker/mk-respond-handler
          task
          (fn [result]
            (worker/savior task result)
            (let [c (count result)
                  end (System/currentTimeMillis)
                  d (- end (get-in @utils/t [n :start]))]
              (swap! utils/t update n merge {:end end :d d})
              (print n "")))))))))

(defn run [{:keys [total count] :as task} threads]
  (println "")
  (println "_____: Start Run :______")
  (stop-all)
  (reset! fs [])
  (reset! t {})
  (worker/clear-run-dir task)
  (let [pages (int (+ 1 (/ total count)))
        ppt   (/ pages threads)
        task (assoc-in task [:run :config ] {:connection-manager acm})
        r (pmap
           (fn [[a] [b]]
             (let [start-page (inc a)
                   end-page (if (>= b pages) pages b)]
               (swap! fs conj (run-thread task start-page end-page))))
           (partition 1 ppt (range pages) )
           (rest (partition 1 ppt (range (+ ppt pages)) )))]
    {:started  (clojure.core/count r)}

    ))
