(ns worker.core-test
  (:require [utils :as utils]
            [worker.kate :as dts]
            [worker.anna :as hs]
            [clojure.test :refer :all]
            [worker.jute :as jute]))

;; (dts/make-date-gaussian "1980" 50)

;; TODO: create primitives fro Identifier, data, Period, Telecom
(def tpl
  {:resourceType "Appointment"
   :bd           "$ randDateGaussian('1980', 50)"
   :period       "$ makeRandPeriod('2020', '{\"minute\": 20}', '{\"year\": 1}')"
   :id           "$ md5(id, salt)"
   :ssn          "$ identifier.*(this.system = \"http://hl7.org/fhir/sid/us-ssn\").value"
   :snils        "$ complicatedHash( identifier.*(this.system = \"http://hl7.org/fhir/sid/us-ssn\").value, salt)"})

(defn add-salt [x s]
  (assoc x :salt s))

(def transform-fn (jute/compile tpl))

(defn salt-transform-fn [salt]
  (fn [x] (transform-fn (add-salt x salt))))

(def task
  {:resourceType "Patient"
   :uri "/Patient"
   :gzip false
   :query-params {}
   :total 124
   :count 10
   :run {:mapper (salt-transform-fn "fofof")
         :destination {:file "/tmp/undefhir"}
         :project {:id "marat6"
                   :base "https://marat6.aidbox.app/"
                   :auth {:basic {:client "undefhir"
                                  :secret "undefhir-super-client"}}}}})

(comment
  (clojure.pprint/pprint

   (transform-fn (add-salt
                  {:id "111111111111111111111"
                   :snils "222-233-445 85"
                   :name "marat"}
                  "22222")))


  (utils/run task 4)
  (utils/print-report @utils/t)

  (dts/make-date-gaussian "1980" 50)
  (dts/make-rand-period   "2020" {:minute 20} {:years 1})

  (hs/md5 "fofofofof")
  (hs/md5 "fofofofof" "1")
  (hs/md5 "fofofofof" "2")
  (hs/simple-hash "fofofofof" "2")

  (map
   (fn [s] (hs/complicated-hash "marat-1991:90" s))
   ["1" "2" "3"])

  (map
   (fn [s] (hs/complicated-hash "9806a625-3134-424f-880d-c64f9a3e7c3c" s))
   ["1" "2" "3"])

  )
