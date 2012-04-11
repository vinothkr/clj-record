(ns clj-record.test-helper
  (:require [clj-record.core :as core]
            [clojure.java.jdbc :as sql])
  (:use clj-record.test-model.config
        clojure.test))

(defmacro defdbtest [name & body]
  `(deftest ~name
    (rolling-back ~@body)))

(defmacro rolling-back [& body]
  `(core/transaction clj-record.test-model.config/db
    (try
      ~@body
      (finally
        (clojure.java.jdbc/set-rollback-only)))))

(defmacro restoring-ref [ref & body]
  `(let [old-value# (deref ~ref)]
    (try
      ~@body
      (finally
        (dosync (ref-set ~ref old-value#))))))

(def valid-manufacturer {:name "Valid Name" :founded "1999" :grade 99})

(defn valid-manufacturer-with [attributes] (merge valid-manufacturer attributes))

(defmulti get-id-key-spec :subprotocol)
(defmethod get-id-key-spec "derby" [db-spec name]
  [:id :int (str "GENERATED ALWAYS AS IDENTITY CONSTRAINT " name " PRIMARY KEY")])
(defmethod get-id-key-spec "sqlserver" [db-spec name]
  [:id :int (str "PRIMARY KEY IDENTITY")])
(defmethod get-id-key-spec "db2" [db-spec name]
  [:id :int (str "GENERATED ALWAYS AS IDENTITY CONSTRAINT " name " PRIMARY KEY")])
(defmethod get-id-key-spec :default [db-spec name]
  [:id "SERIAL UNIQUE PRIMARY KEY"])

(defmulti get-table-spec :subprotocol)
(defmethod get-table-spec "mysql" [db-spec]
  "Engine=InnoDB")
(defmethod get-table-spec :default [db-spec]
  nil)

(def table-specs
  { :manufacturers
      [ (get-id-key-spec db "manufacturer_pk")
        [:name    "VARCHAR(32)" "NOT NULL"]
        [:founded "VARCHAR(4)"]
        [:grade   :int]
        :table-spec (get-table-spec db)]
   :productos
      [ (get-id-key-spec db "product_pk")
        [:name            "VARCHAR(32)" "NOT NULL"]
        [:price           :int]
        [:manufacturer_id :int "NOT NULL"]
        :table-spec (get-table-spec db)]
   :person
      [ (get-id-key-spec db "person_pk")
        [:name             "VARCHAR(32) NOT NULL"]
        [:mother_id        :int]
        [:father_person_id :int]
        :table-spec (get-table-spec db)]
   :thing_one
      [ (get-id-key-spec db "thing_one_pk")
        [:name            "VARCHAR(32)" "NOT NULL"]
        [:owner_person_id   :int]
        :table-spec (get-table-spec db)]
   :thing_two
      [ (get-id-key-spec db "thing_two_pk")
        [:thing_one_id   :int "NOT NULL"]
        :table-spec (get-table-spec db)]
   :thing_three
      [ (get-id-key-spec db "thing_one_pk")
        [:thing_two_id   :int "NOT NULL"]
        :table-spec (get-table-spec db)]})

(defn drop-tables
  ([] (drop-tables (keys table-specs)))
  ([tables]
    (try
      (doseq [table tables]
        (sql/drop-table table))
      (catch Exception e))))

(defn create-tables
  ([] (create-tables (keys table-specs)))
  ([tables]
    (doseq [table-pair (select-keys table-specs tables)]
      (apply sql/create-table (first table-pair) (second table-pair)))))

(defn reset-db []
  (println "resetting" (db :subprotocol))
  (sql/with-connection db
    (sql/transaction
      (drop-tables)
      (create-tables)))
  (println "database reset"))

