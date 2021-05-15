(ns worker.anna
  (:require [utils :as utils]))

(def task
  {:resourceType "Patient"          ;; Target resource type
   :uri "/Patient"                  ;; Uri
   :gzip false                      ;; Gzip result or not
   :query-params {}                 ;; Search query for example {:name "marat"}
   :total 124                       ;; Total amout of resource
   :count 10                        ;; Count per page
   :run {;; :mapper - mapping function. In this case we take only id and name from patient
         ;; for raw Patient data you can remove this field
         :mapper  (fn [resource] ;;(select-keys resource [:id :name])
                                 ;;{:id (:id resource)})
                    {:full-name (str (get-in resource [:name 0 :family]) " " (get-in resource [:name 0 :given 0]))
                     :registration-address (str (get-in resource [:address 0 :country]) ", " (get-in resource [:address 0 :city]) ", " (get-in resource [:address 0 :line 0]))
                     :date-of-birth (get-in resource [:birthDate])
                     :sex (get-in resource [:gender])
                     :med-card-info (get-in resource [:identifier 1 :value])
                     :social-security (:value (first (filter (fn [x] (= (:system x) "http://hl7.org/fhir/sid/us-ssn"))
                                                             (get-in resource [:identifier]))))
                     :contact-phone (get-in resource [:telecom 0 :value])})

         ;; Result files destination dirrectory
         :destination {:file "/tmp/undefhir"}

         ;; Fhir server connection
         :project {:id "hseaidbox"                            ;; Just Id of project
                   :base "https://hseaidbox.aidbox.app/"      ;; Base of FHIR server
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
