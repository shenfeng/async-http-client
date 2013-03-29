(ns me.shenfeng.httpclient-test
  (:use clojure.test
        [ring.adapter.jetty :only [run-jetty]]
        (compojure [core :only [defroutes GET PUT PATCH DELETE POST HEAD DELETE ANY context]]
                   [handler :only [site]]
                   [route :only [not-found]]))
  (:import me.shenfeng.http.HttpClient
           java.net.URI
           me.shenfeng.http.HttpClientConfig))

(defroutes test-routes
  (GET "/get" [] "hello world")
  (POST "/post" [] "hello world"))

(use-fixtures :once
  (fn [f]
    (let [jetty (run-jetty (site test-routes) {:port 14347
                                               :join? false})]
      (try (f) (finally (.stop jetty))))))

(deftest test-http-client
  (let [host "http://127.0.0.1:14347"
        client (HttpClient. (HttpClientConfig.))]
    (is (= 200 (.getCode (.getStatus (.get (.execGet client (str host "/get")))))))
    (is (= 200 (.getCode (.getStatus (.get (.execPost client (URI. (str host "/post"))
                                                      {} {}))))))))
