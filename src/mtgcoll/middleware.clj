(ns mtgcoll.middleware
  (:require
    [clojure.tools.logging :as log]
    [webtools.response :as response]
    [mtgcoll.auth :as auth]))

(defn wrap-authenticated
  [handler]
  (fn [request]
    (if (or (not (auth/using-authorization?))
            (get-in request [:session :user]))
      (handler request)
      (-> (response/content "unauthorized")
          (response/status 401)))))

(defn wrap-api-exceptions
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception ex
        (log/error ex "Unhandled exception.")
        (-> (response/json {:status  "error"
                            :message (.getMessage ex)})
            (response/status 500))))))
