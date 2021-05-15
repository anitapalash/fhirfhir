(ns worker.fhir-server)

(defn server-auth [task]
  {:basic-auth [(get-in task [:run :project :auth :basic :client])
                (get-in task [:run :project :auth :basic :secret])]})

(defn server-base [task]
  (get-in task [:run :project :base]))

(defn task-url [task]
  (str (server-base task)
       (get-in task [:uri])))

(defn task-query-params [task]
  {:_count (get-in task [:run :count])})

(defn task-params [task]
  (merge {:accept :json
          :async  :true
          :as     :json}
         (get-in task [:run :config])
         (server-auth task)
         {:query-params
          (merge
           (task-query-params task)
           (:query-params task))}))
