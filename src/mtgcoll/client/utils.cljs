(ns mtgcoll.client.utils
  (:require
    [cljs.pprint :as pprint]
    [clojure.string :as string]))

(defn format-date
  [date]
  (if (and date
           (instance? js/Date date))
    (let [year  (.getFullYear date)
          month (inc (.getMonth date))
          day   (.getDate date)]
      (str year "-"
           (if (< month 10) (str "0" month) month) "-"
           (if (< day 10) (str "0" day) day)))))

(defn format-datetime
  [date]
  (if (and date
           (instance? js/Date date))
    (let [year    (.getFullYear date)
          month   (inc (.getMonth date))
          day     (.getDate date)
          hour    (.getHours date)
          minutes (.getMinutes date)
          seconds (.getSeconds date)]
      (str year "-"
           (if (< month 10) (str "0" month) month) "-"
           (if (< day 10) (str "0" day) day) " "
           (if (< hour 10) (str "0" hour) hour) ":"
           (if (< minutes 10) (str "0" minutes) minutes) ":"
           (if (< seconds 10) (str "0" seconds) seconds)))))

(defn get-field-value
  [e]
  (if e
    (let [target     (.-target e)
          tag-name   (.-tagName target)
          input-type (.-type target)]
      (if (and (= "INPUT" tag-name)
               (= "checkbox" input-type))
        (if (.-checked target) (.-value target))
        (.-value target)))))

(defn valid-float?
  [s]
  (boolean
    (if-not (string/blank? s)
      (re-matches #"^([0-9]+(\.[0-9]+)?)" s))))

(defn valid-integer?
  [s]
  (boolean
    (if-not (string/blank? s)
      (re-matches #"^([0-9]+)" s))))

(defn format-number
  [n]
  (if n
    (string/replace (str n) #"\B(?=(\d{3})+(?!\d))" ",")))

(defn format-currency
  [n]
  (if n
    (str "$" (pprint/cl-format nil "~,2f" n))))
