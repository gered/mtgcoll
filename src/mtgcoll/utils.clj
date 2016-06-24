(ns mtgcoll.utils
  (:require
    [clojure.string :as string])
  (:import
    (java.io ByteArrayInputStream InputStream)
    (java.text Normalizer Normalizer$Form)))

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
