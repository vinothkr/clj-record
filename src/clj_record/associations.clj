(ns clj-record.associations
  (:use clj-record.util)
  (:require clj-record.query))

(defn -associate [model-name records foreign-key]
  (clj-record.core/find-records model-name {foreign-key (apply clj-record.query/in (map :id records))}))

(defn more-eager [records options]
  (cond (and (map? options) (not (empty? options)))
        (let [even-more-eager (first (keys options))] (recur (even-more-eager records (options even-more-eager)) (dissoc options even-more-eager)))
        (and (vector? options) (not (empty? options)))
        (recur ((first options) records) (rest options))
        (fn? options) (options records)
        :default records))

(defn eager-fetch
  ([model-name foreign-key attribute-name records options]
     (let [fetched-records (more-eager (-associate model-name records foreign-key) options)]
       (map (fn [record] (conj {attribute-name (filter (fn [fetched] (= (:id record) (foreign-key fetched))) fetched-records)} record)) records)))
  ([model-name foreign-key attribute-name records]
     (eager-fetch model-name foreign-key attribute-name records [])))

(defn expand-init-option
  "Called via init-model when an :associations option group is encountered.
  Options are alternating key/value pairs."
  [model-name association-type-sym association-name & options]
  (let [assoc-fn (ns-resolve 'clj-record.associations association-type-sym)]
    (apply assoc-fn model-name association-name options)))

(defn has-many
  "Defines an association to a model whose name is infered by singularizing association-name.
  In ns foo's init-model, (has-many bars) will define find-bars and destroy-bars functions in foo,
  each of which take a foo record and find or destroy bars by {:foo_id (record :id)}.

  Options are alternating key/value pairs. Supported options:

    :fk foreign_key_col_name
    :model target-model-name
  
  Called indirectly via clj-record.core/init-model."
  [model-name association-name & options]
  (let [opts (apply hash-map options)
        associated-model-name (str (or (:model opts) (singularize (name association-name))))
        foreign-key-attribute (keyword (or (:fk opts) (str (dashes-to-underscores model-name) "_id")))
        find-fn-name (symbol (str "find-" association-name))
	eager-fetch-fn-name (symbol (str "eager-fetch-" association-name))
	attribute (keyword association-name)
        destroy-fn-name (symbol (str "destroy-" association-name))]
    `(do
       (defn ~find-fn-name [record#]
        (clj-record.core/find-records ~associated-model-name {~foreign-key-attribute (record# :id)}))
      (defn ~destroy-fn-name [record#]
        (clj-record.core/destroy-records ~associated-model-name {~foreign-key-attribute (record# :id)}))
      (defn ~eager-fetch-fn-name
        ([records#] (clj-record.associations/eager-fetch ~associated-model-name ~foreign-key-attribute ~attribute records#))
        ([records# options#] (clj-record.associations/eager-fetch ~associated-model-name ~foreign-key-attribute ~attribute records# options#))))))

(defn belongs-to
  "Defines an association to a model named association-name.
  In ns bar's init-model, (belongs-to foo) will define find-foo in bar.

  Options are alternating key/value pairs. Supported options:

    :fk foreign_key_col_name
    :model target-model-name
  
  If model is specified and fk is not, fk name is inferred from the
  association name. For example,

    (belongs-to mother :model person)

  will assume the foreign key is mother_id.

  Called indirectly via clj-record.core/init-model."
  [model-name association-name & options]
  (let [opts (apply hash-map options)
        associated-model-name (str (or (:model opts) association-name))
        find-fn-name (symbol (str "find-" association-name))
        foreign-key-attribute (keyword (or
                                        (:fk opts)
                                        (str (dashes-to-underscores (str association-name)) "_id")))]
    `(defn ~find-fn-name [record#]
      (clj-record.core/get-record ~associated-model-name (~foreign-key-attribute record#)))))
