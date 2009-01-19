(ns clj-record.test.callbacks-test
  (:require
    [clj-record.test.model.manufacturer :as manufacturer])
  (:use clojure.contrib.test-is
        clj-record.test.test-helper))


(defdbtest before-save-can-transform-the-record-before-create
  (let [m (manufacturer/create {:name "A" :founded "68"})]
    (is (= "1968" (m :founded)))))

(defdbtest before-save-can-transform-the-record-before-update
  (let [m (manufacturer/create {:name "A" :founded "1968"})
        id (m :id)]
    (manufacturer/update {:id id :founded "68"})
    (is (= "1968" ((manufacturer/get-record id) :founded)))))