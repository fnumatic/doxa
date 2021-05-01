(ns ribelo.example.pull
  (:require [ribelo.doxa :as dx]))

(def people-docs
  [{:db/id 1, :name "Petr", :aka ["Devil" "Tupen"] :child [[:db/id 2] [:db/id 3]]}
   {:db/id 2, :name "David", :father [[:db/id 1]]}
   {:db/id 3, :name "Thomas", :father [[:db/id 1]]}
   {:db/id 4, :name "Lucy" :friend [[:db/id 5]], :enemy [[:db/id 6]]}
   {:db/id 5, :name "Elizabeth" :friend [[:db/id 6]], :enemy [[:db/id 7]]}
   {:db/id 6, :name "Matthew", :father [[:db/id 3]], :friend [[:db/id 7]], :enemy [[:db/id 8]]}
   {:db/id 7, :name "Eunan", :friend [[:db/id 8]], :enemy [[:db/id 4]]}
   {:db/id 8, :name "Kerri"}
   {:db/id 9, :name "Rebecca"}])

(def db (dx/create-dx people-docs))

;; * eql
(dx/pull db {[:db/id 1] [:name :aka]})
;; => {:name "Petr", :aka ["Devil"]}

;; * datomic like pull syntax

(dx/pull db [:name :aka] [:db/id 1])
;; => {:name "Petr", :aka ["Devil"]}

;; ** simple query

(dx/pull db  [:name :father :db/id] [:db/id 6])
;; => {:name "Matthew", :father [:db/id 3], :db/id 6}

;; ** pull many

(dx/pull db [:name] [[:db/id 1] [:db/id 5] [:db/id 7] [:db/id 9]])
;; => [{:name "Petr"} {:name "Elizabeth"} {:name "Eunan"} {:name "Rebecca"}]

;; ** reverse search
(dx/pull db [:name :_child] [:db/id 2])
;; => {:name "David", :_child [:db/id 1]}

(dx/pull db [:name {:_child [:name]}] [:db/id 2])
;; => {:name "David", :_child {:name "Petr"}}

;; ** reverse non-component references yield collections
(dx/pull db '[:name :_father] [:db/id 3])
;; => {:name "Thomas", :_father [:db/id 6]}

(dx/pull db '[:name :_father] [:db/id 1])
;; => {:name "Petr", :_father [[:db/id 3] [:db/id 2]]}

(dx/pull db '[:name {:_father [:name]}] [:db/id 3])
;; => {:name "Thomas", :_father {:name "Matthew"}}

(dx/pull db '[:name {:_father [:name]}] [:db/id 1])
;; => {:name "Petr", :_father [{:name "Thomas"} {:name "David"}]}

;; ** wildcard

(dx/pull db [:*] [:db/id 1])
;; =>
;; {:db/id 1, :name "Petr", :aka ["Devil" "Tupen"], :child [[:db/id 2] [:db/id 3]]}

(dx/pull db [:* :_child] [:db/id 2])
;; => {:db/id 2, :name "David", :father [:db/id 1], :_child [:db/id 1]}

;; ** missing attrs are dropped

(dx/pull db [:name {:child [:name]}] [:db/id 2])
;; => {:name "David"}

;; ** non matching results are removed from collections

(dx/pull db [:name {:child [:foo]}] [:db/id 1])
;; => {:name "Petr", :child []}