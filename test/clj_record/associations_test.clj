(ns clj-record.associations-test
  (:require
    [clj-record.test-model.manufacturer :as manufacturer]
    [clj-record.test-model.product :as product]
    [clj-record.test-model.person :as person]
    [clj-record.test-model.thing-one :as thing-one]
    [clj-record.test-model.thing-two :as thing-two]
    [clj-record.test-model.thing-three :as thing-three])
  (:use clojure.test
        clj-record.test-helper))


(defdbtest belongs-to-creates-find-function
  (let [humedai (manufacturer/create (valid-manufacturer-with {:name "Humedai Automotive"}))
        s3000xi (product/create {:name "S-3000xi" :manufacturer_id (:id humedai)})]
    (is (= humedai (product/find-manufacturer s3000xi)))))

(defdbtest has-many-creates-a-find-function
  (let [humedai (manufacturer/create (valid-manufacturer-with {:name "Humedai Automotive"}))
        s3000xi (product/create {:name "S-3000xi" :manufacturer_id (:id humedai)})
        s3000xl (product/create {:name "S-3000xl" :manufacturer_id (:id humedai)})]
    (is (= [s3000xi s3000xl] (manufacturer/find-products humedai)))))

(defdbtest has-many-creates-a-destroy-function
  (let [humedai (manufacturer/create (valid-manufacturer-with {:name "Humedai Automotive"}))
        s3000xi (product/create {:name "S-3000xi" :manufacturer_id (:id humedai)})
        s3000xl (product/create {:name "S-3000xl" :manufacturer_id (:id humedai)})]
    (manufacturer/destroy-products humedai)
    (is (empty? (manufacturer/find-products humedai)))))

(defdbtest belongs-to-custom-fk-and-model
  (let [mother-rec (person/create {:name "mom"})
        father-rec (person/create {:name "dad"})
        kid-rec (person/create {:name "kid" :mother_id (:id mother-rec) :father_person_id (:id father-rec)})]
    (is (= mother-rec (person/find-mother kid-rec)))
    (is (= father-rec (person/find-father kid-rec)))))

(defdbtest has-many-custom-fk-and-model
  (let [person-rec (person/create {:name "phred"})
        thing-rec1 (thing-one/create {:name "one" :owner_person_id (:id person-rec)})
        thing-rec2 (thing-one/create {:name "two" :owner_person_id (:id person-rec)})]
    (is (= [thing-rec1 thing-rec2] (person/find-things person-rec)))))

(comment
(defdbtest find-records-can-do-eager-fetching-of-has-many-association
  (let [manu1 (manufacturer/create (valid-manufacturer-with {:name "manu1" :grade 99}))
        prod1 (product/create {:name "prod1" :manufacturer_id (:id manu1)})
        prod2 (product/create {:name "prod2" :manufacturer_id (:id manu1)})
        manu2 (manufacturer/create (valid-manufacturer-with {:name "manu2" :grade 99}))
        prod3 (product/create {:name "prod3" :manufacturer_id (:id manu2)})
        prod4 (product/create {:name "prod4" :manufacturer_id (:id manu2)})]
    (let [[eager-manu1 eager-manu2] (manufacturer/find-records {:grade 99} {:include [:products]})]
      (is (= "manu1" (:name eager-manu1)))
      (is (= "manu2" (:name eager-manu2)))
      (is (= [prod1 prod2] (:products eager-manu1)))
      (is (= [prod3 prod4] (:products eager-manu2))))))
)


;This is very primitive implementation of eager fetch
(defdbtest find-records-can-do-eager-fetching-of-has-many-association
  (let [manu1 (manufacturer/create (valid-manufacturer-with {:name "manu1" :grade 99}))
        prod1 (product/create {:name "prod1" :manufacturer_id (:id manu1)})
        prod2 (product/create {:name "prod2" :manufacturer_id (:id manu1)})
        manu2 (manufacturer/create (valid-manufacturer-with {:name "manu2" :grade 99}))
        prod3 (product/create {:name "prod3" :manufacturer_id (:id manu2)})
        prod4 (product/create {:name "prod4" :manufacturer_id (:id manu2)})]
    (let [[eager-manu1 eager-manu2] (manufacturer/eager-fetch-products (manufacturer/find-records {:grade 99}))]
      (is (= "manu1" (:name eager-manu1)))
      (is (= "manu2" (:name eager-manu2)))
      (is (= [prod1 prod2] (:products eager-manu1)))
      (is (= [prod3 prod4] (:products eager-manu2))))))

;This allows nested eager-fetching (TODO: make a cleaner interface that exposes this)
(defdbtest find-records-can-do-nested-eager-fetching-of-has-many-associations
  (let [person1 (person/create {:name "a person"})
        thing-one1 (thing-one/create {:name "thing-one1" :owner_person_id (:id person1)})
        thing-one2 (thing-one/create {:name "thing-one2" :owner_person_id (:id person1)})
        thing-two1 (thing-two/create {:thing_one_id (:id thing-one1)})
        thing-two2 (thing-two/create {:thing_one_id (:id thing-one1)})
        thing-three1 (thing-three/create {:thing_two_id (:id thing-two1)})
        thing-three2 (thing-three/create {:thing_two_id (:id thing-two1)})]
    (let [person-fetched (person/find-record {:name "a person"}
                                              {person/eager-fetch-things
                                               {thing-one/eager-fetch-thing-twos
                                                [thing-two/eager-fetch-thing-threes]}})]
      (is (= "a person" (:name person-fetched)))
      (is (= ["thing-one1" "thing-one2"] (map :name (:things person-fetched))))
      (is (= (map :id [thing-two1 thing-two2]) (map :id (:thing-twos (first (:things person-fetched))))))
      (is (= (map :id [thing-three1 thing-three2]) (map :id (:thing-threes (first (:thing-twos (first (:things person-fetched)))))))))))
                               