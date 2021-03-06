(ns bouncer.validators-test
  (:use clojure.test)
  (:use [bouncer.validators :only [defvalidator defvalidatorset]])
  (:require [bouncer
             [core :as core]
             [validators :as v]]))

(defvalidatorset addr-validator-set
  :postcode [v/required v/number]
  :street    v/required
  :country   v/required
  :past      (v/every #(not(nil? (:country %)))))


(defvalidatorset addr-validator-set+custom-messages
  :postcode [(v/required :message "required") (v/number :message "number")]
  :street    (v/required :message "required")
  :country   (v/required :message "required")
  :past      (v/every #(not(nil? (:country %))) :message "every"))

(deftest validator-sets
  (testing "composable validators"
    (is (core/valid? {:address {:postcode 2000
                                  :street   "Crown St"
                                  :country  "Australia"
                                  :past [{:country "Spain"} {:country "Brazil"}]}}
                     :address addr-validator-set))

    (is (not (core/valid? {}
                       :address addr-validator-set)))
    
    (let [errors-map {:address {
                                :postcode '("postcode must be a number"
                                            "postcode must be present")
                                :street    '("street must be present")
                                :country   '("country must be present")
                                :past '("All items in past must satisfy the predicate")
                                }}
          invalid-map {:address {:postcode ""
                                 :past [{:country nil} {:country "Brasil"}]}}]
      (is (= errors-map
             (first (core/validate invalid-map
                                   :address addr-validator-set))))))

  (testing "custom messages in validator sets"
    (let [errors-map {:address {
                                :postcode '("number"
                                            "required")
                                :street    '("required")
                                :country   '("required")
                                :past '("every")
                                }}
          invalid-map {:address {:postcode ""
                                 :past [{:country nil} {:country "Brasil"}]}}]
      (is (= errors-map
             (first (core/validate invalid-map
                                   :address addr-validator-set+custom-messages))))))
  
  (testing "validator sets and standard validators together"
    (let [errors-map {:age '("age isn't 29" "age must be a number" "age must be present")
                      :name '("name must be present")
                      :passport {:number '("number must be a positive number")}
                      :address {
                                :postcode '("postcode must be a number"
                                            "postcode must be present")
                                :street    '("street must be present")
                                :country   '("country must be present")
                                :past '("All items in past must satisfy the predicate")
                                }}
          invalid-map {:name nil
                       :age ""
                       :passport {:number -7 :issued_by "Australia"}
                       :address {:postcode ""
                                 :past [{:country nil} {:country "Brasil"}]}}]
      (is (= errors-map
             (first (core/validate invalid-map
                                   :name v/required
                                   :age [v/required
                                         v/number
                                         (v/custom #(= 29 %) :message "age isn't 29")]
                                   [:passport :number] v/positive 
                                   :address addr-validator-set)))))))



(deftest range-validator
  (testing "presence of value in the given range"
    (is (core/valid? {:age 4}
                  :age (v/member (range 5))))
    (is (not (core/valid? {:age 5}
                          :age (v/member (range 5)))))))