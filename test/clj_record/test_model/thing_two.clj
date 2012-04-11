(ns clj-record.test-model.thing-two
  (:require clj-record.boot)
  (:use clj-record.test-model.config))

(clj-record.core/init-model
 :table-name "thing_two"
 (:associations
  (has-many thing-threes :fk thing_two_id :model thing-three)
  (belongs-to thing-one)))
