(ns clj-record.test-model.thing-three
  (:require clj-record.boot)
  (:use clj-record.test-model.config))

(clj-record.core/init-model
 :table-name "thing_three"
  (:associations
   (belongs-to thing-twos)))
