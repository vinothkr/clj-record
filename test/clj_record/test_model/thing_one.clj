(ns clj-record.test-model.thing-one
  (:require clj-record.boot)
  (:use clj-record.test-model.config))

(clj-record.core/init-model
 :table-name "thing_one"
  (:associations
    (has-many thing-twos :fk thing_one_id :model thing-two)
    (belongs-to person :fk owner_person_id)))
