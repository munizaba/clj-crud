(ns clj-crud.sendgrid-test
  (:require [clojure.tools.logging :refer [info debug spy]]
            [clojure.test :refer :all]
            [clojure.edn :as edn]
            [clj-crud.test-core :as tc]
            [peridot.core :refer :all]
            [kerodon.core :as k]
            [kerodon.test :refer :all]
            [ring.util.io :as ring-io]))

(deftest sendgrid-test
  (let [todo-body "My todo by email"
        form-body (str "--TESTTEST\r\n"
                       "Content-Disposition: form-data;"
                       "name=\"subject\"\r\n\r\n"
                       "user1\r\n"
                       "--TESTTEST\r\n"
                       "Content-Disposition: form-data;"
                       "name=\"text\"\r\n\r\n"
                       todo-body
                       "\r\n"
                       "--TESTTEST--")]
    (-> (session (tc/reuse-handler))
        (content-type "multipart/form-data; boundary=TESTTEST")
        (request "/inboundmail"
                 :request-method :post
                 :headers {} #_{"Content-Length" (count form-body)}
                 :body (ring-io/string-input-stream form-body))
        (has (status? 200)))
    (-> (session (tc/reuse-handler))
        (k/visit "/login")
        (k/fill-in "Name" "User 1")
        (k/fill-in "Password" "user1")
        (k/press "Login")
        (follow-redirect)
        (request "/todos/user1/todos")
        (has (status? 200))
        ((fn [res]
           (let [todos (-> (get-in res [:response :body]) edn/read-string :todos)]
             (is (= (-> todos last :text) todo-body))
             ))))))
