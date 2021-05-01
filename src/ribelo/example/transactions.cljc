(ns ribelo.example.transactions
  (:require [ribelo.doxa :as dx]))

(def data [{:db/id 1 :name "Petr" :aka ["Devil"]}])

(def db (dx/create-dx data)
  ;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil"]}}}
  )

;; * put
;; ** add entity

(dx/commit {} [[:dx/put {:db/id 1 :name "David" :aka ["Devil"]}]])
;; => #:db{:id {1 {:db/id 1, :name "David", :aka ["Devil"]}}}

(dx/commit {} [[:dx/put [:db/id 1] {:name "David" :aka ["Devil"]}]])
;; => #:db{:id {1 {:name "David", :aka ["Devil"], :db/id 1}}}

;; ** single keyword change

(dx/commit db [[:dx/put [:db/id 1] :name "David"]])
;; => #:db{:id {1 {:db/id 1, :name "David", :aka ["Devil"]}}}

(dx/commit db [[:dx/put [:db/id 1] :aka ["Tupen"]]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Tupen"]}}}

;; ** add data with autonormalization

(dx/commit db [[:dx/put [:db/id 1] :friend [{:db/id 2 :name "Ivan"} {:db/id 3 :name "Lucy"}]]])
;; =>
;; #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil"], :friend [[:db/id 2] [:db/id 3]]},
;;           2 {:db/id 2, :name "Ivan"},
;;           3 {:db/id 3, :name "Lucy"}}}


;; * delete
;; ** delete entity

(dx/commit db [[:dx/delete [:db/id 1]]])
;; => {}

;; ** delete keyword

(dx/commit db [[:dx/delete [:db/id 1] :aka]])

;; ** remove elem from vector

(dx/commit db [[:dx/delete [:db/id 1] :aka "Devil"]])
;; => #:db{:id {1 {:db/id 1, :name "Petr"}}}

;; ** remove an invalid key

(dx/commit db [[:dx/delete [:db/id 1] :AKA "Devil"]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil"]}}}

;; * conj
;; because the database is schemeless, if we want to add something to the vector
;; we have to use conj

;; ** add elem

(dx/commit db [[:dx/conj [:db/id 1] :aka "Tupen"]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil" "Tupen"]}}}

(dx/commit db [[:dx/conj [:db/id 1] :name "Ivan"]])
;; => #:db{:id {1 {:db/id 1, :name ["Petr" "Ivan"], :aka ["Devil"]}}}

;; ** with autonormalisation

(dx/commit db [[:dx/conj [:db/id 1] :friend {:db/id 2 :name "Ivan"}]])
;; =>
;; #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil"], :friend [[:db/id 2]]},
;;           2 {:db/id 2, :name "Ivan"}}}

(dx/commit db [[:dx/conj [:db/id 1] :friend [{:db/id 2 :name "Ivan"} {:db/id 3 :name "Lucy"}]]])
;; =>
;; #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil"], :friend [[:db/id 2] [:db/id 3]]},
;;           2 {:db/id 2, :name "Ivan"}, 3
;;           {:db/id 3, :name "Lucy"}}}

;; * update
;; map can be updated by any function

(dx/commit db [[:dx/update [:db/id 1] assoc :aka "Tupen"]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka "Tupen"}}}

(dx/commit db [[:dx/update [:db/id 1] :aka conj "Tupen"]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil" "Tupen"]}}}

;; * match
;; just like in crux, we can use match
;; if data match, db is returned unchanged, otherwise nil

;; ** match entity

(dx/commit db [[:dx/match [:db/id 1] {:db/id 1 :name "Petr", :aka ["Devil"]}]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil"]}}}

;; ** match keyword
(dx/commit db [[:dx/match [:db/id 1] :aka ["Devil"]]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil"]}}}

;; ** conditional put

(dx/commit db [[:dx/match [:db/id 1] :aka ["Devil"]]
               [:dx/put   [:db/id 1] :aka ["Tupen"]]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Tupen"]}}}

;; ** conditional delete
(dx/commit db [[:dx/match [:db/id 1]  :aka ["Tupen"]]
               [:dx/delete [:db/id 1] :aka]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil"]}}}

;; transactions are dropped until the next valid match occurs

(dx/commit db [[:dx/match [:db/id 1] :aka ["Tupen"]]
               [:dx/put [:db/id 1] :age 15]
               [:dx/match [:db/id 1] :name "Petr"]
               [:dx/put [:db/id 1] :sex :male]])
;; => #:db{:id {1 {:db/id 1, :name "Petr", :aka ["Devil"], :sex :male}}}

(comment
  (+ 2 2)
  ,)