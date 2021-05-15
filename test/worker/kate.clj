(ns worker.kate
  (:require
   [utils :as utils]))

(def task
  {:resourceType "Patient"          ;; Target resource type
   :uri "/Patient"                  ;; Uri
   :gzip false                      ;; Gzip result or not
   :query-params {}                 ;; Search query for example {:name "marat"}
   :total 124                       ;; Total amout of resource
   :count 10                        ;; Count per page
   :run {;; :mapper - mapping function. In this case we take only id and name from patient
         ;; for raw Patient data you can remove this field
         :mapper  (fn [resource] (select-keys resource [:id :name]))

         ;; Result files destination dirrectory
         :destination {:file "/tmp/undefhir"}

         ;; Fhir server connection
         :project {:id "mybox1"                            ;; Just Id of project
                   :base "https://mybox1.aidbox.app/"      ;; Base of FHIR server
                   :auth {:basic {:client "basic"    ;; Basic Auth client name
                                  :secret "secret"  ;; Basic Auth client secret
                                  }}}}})


(comment
  ;; Run processing in 4 threads
  (utils/run task 4)
  ;; Stop all threads
  (utils/stop-all)
  ;; Print time report
  (utils/print-report @utils/t)

)
