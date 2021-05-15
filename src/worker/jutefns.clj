(ns worker.jutefns
  (:require
   [clojure.string :as str]
   [clj-time.local :as l]
   [clj-time.core :as t]
   [clj-time.coerce :as c]
   [clj-time.format :as f]
   [clojure.math.numeric-tower :as math]
   [clojure.core.matrix.random :as g]
   )
  (:import
   [java.security MessageDigest]
   [java.math BigInteger])
  )

;; Dates
(def iso-fmt  (f/formatter-local "yyyy-MM-dd HH:mm:ss"))

(defn to-iso [timestamp]
  (->> timestamp
       c/from-long
       (f/unparse iso-fmt)))

(defn make-date-gaussian [year dispersion]
  (-> (g/rand-gaussian
       (c/to-long (str year))
       (* (Math/sqrt dispersion) 31556926000))
      long
      to-iso))

(defn make-dates-binomial [year dispersion times]
  ;; TODO: Write me
  (/ 1 0))

(defn make-conv [d]
  (cond
    (:minute d)
    (* (:minute d) 60000)

    (:day d)
    (* (:day d) 86400000)

    (:month d)
    (* (:month d) 2629743000)

    (:year d)
    (* (:year d) 31556926000)

    :else 0))


(defn make-rand-period [start delta dispersion]
  (let [first-date  (long (g/rand-gaussian (c/to-long start) (make-conv dispersion)))
        last-date   (+ first-date (make-conv delta))]
    (mapv to-iso [first-date  last-date])))

;; Hashes
(defn md5 [s & [salt]]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes (str salt s)))]
    (format "%032x" (BigInteger. 1 raw))))

(defn complicated-hash [obj & [salt]]
  (->> (str obj)
       (map-indexed
        (fn [idx i] [idx i]
          (cond (= (re-matches #"[^a-zA-z0-9]" (str i)) (str i))
                (str i)
                (= (re-matches #"[0-9]" (str i)) (str i))
                ;; TODO: fix digest
                (str (if (neg? (rem (hash (+ (- (count (str obj)) idx) (int i))) 10))
                       (- (rem (hash (+ (- (count (str obj)) idx) (int i))) 10))
                       (rem (hash (+ (- (count (str obj)) idx) (int i))) 10)))
                :else
                (subs (md5 (+ (- (count (str obj)) idx) (int i)) salt)
                      (- (count (md5 (+ (- (count (str obj)) idx) (int i)))) 1)))))
       str/join))
