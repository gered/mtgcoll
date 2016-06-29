(ns mtgcoll.utils
  (:require
    [clojure.string :as string]
    [clojure.java.io :as io]
    [clj-http.client :as http])
  (:import
    (java.io ByteArrayInputStream InputStream)
    (java.text Normalizer Normalizer$Form)
    (java.util Properties)))

(def chrome-osx-request-headers
  {"User-Agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.84 Safari/537.36"
   "Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"
   "Accept-Encoding" "gzip, deflate, sdch"
   "Accept-Language" "en-US,en;q=0.8,en-CA;q=0.6"
   "Cache-Control" "no-cache"
   "Connection" "keep-alive"
   "Pragma" "no-cache"})

(defn string->stream
  ^InputStream [^String s]
  (-> s .getBytes ByteArrayInputStream.))

(defn parse-currency-string
  [^String s]
  (if-not (string/blank? s)
    (BigDecimal. (.replaceAll s "[^\\d\\.]" ""))))

(defn normalize-string
  [^String s]
  (if s
    (-> s
        (Normalizer/normalize Normalizer$Form/NFD)
        (.replace "Æ" "Ae")
        (.replace "‘" "'")
        (.replace "’" "'")
        (.replace "“" "\"")
        (.replace "”" "\"")
        (.replace "–" "-")
        (.replaceAll "[^\\p{ASCII}]" ""))))

(defn file-exists?
  [filename]
  (if-not (string/blank? filename)
    (let [file (io/file filename)]
      (.exists file))))

(defn download-as-byte-array
  [url]
  (let [response (http/get url {:headers chrome-osx-request-headers :as :byte-array})
        body     (:body response)]
    (if-not (empty? body)
      body)))

(defn pad-string
  [n s & [c]]
  (let [c (or c \space)]
    (if (< (count s) n)
      (->> (repeat c) (concat s) (take n) (string/join))
      s)))

(defn get-app-version
  []
  (if-let [version (System/getProperty "mtgcoll.version")]
    version
    (-> (doto (Properties.)
          (.load (-> "META-INF/maven/%s/%s/pom.properties"
                     (format "mtgcoll" "mtgcoll")
                     (io/resource)
                     (io/reader))))
        (.get "version"))))
