(ns mtgcoll.middleware
  (:require
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