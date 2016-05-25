(ns posh.tree.db
  (:require [datascript.core :as d]
            [posh.util :as util]
            [posh.datom-matcher :as dm]
            [posh.pull-analyze :as pa]
            [posh.q-analyze :as qa]))

(defn get-parent-db [poshdb]
  (if (= (first poshdb) :db)
    nil
    (second poshdb)))

(defn get-db-path [poshdb]
  (loop [path []
         pdb  poshdb]
    (if pdb
      (recur (cons pdb path) (get-parent-db pdb))
      path)))

(defn conn-id->conn [posh-tree conn-id]
  (get (:conns posh-tree) conn-id))

(defn conn-id->schema [posh-tree conn-id]
  (get (:schemas posh-tree) conn-id))

(defn conn-id->db [posh-tree conn-id]
  (get (:dbs posh-tree) conn-id))

(defn poshdb->conn-id [poshdb]
  (if (= (first poshdb) :db)
    (second poshdb)
    (recur (get-parent-db poshdb))))

(defn conn-id->attrs [posh-tree conn-id]
  {:conn    (conn-id->conn posh-tree conn-id)
   :schema  (conn-id->schema posh-tree conn-id)
   :db      (conn-id->db posh-tree conn-id)
   :conn-id conn-id})

(defn poshdb->attrs [posh-tree poshdb]
  (conn-id->attrs posh-tree (poshdb->conn-id poshdb)))

(defn make-filter-pred [tx-patterns]
  (fn [_ datom]
    (dm/datom-match? tx-patterns datom)))

(defn poshdb->db [{:keys [dcfg cache] :as posh-tree}  poshdb]
  (if (= (first poshdb) :db)
    (conn-id->db posh-tree (second poshdb))
    ((:filter dcfg)
     (poshdb->db posh-tree (get-parent-db poshdb))
     (make-filter-pred (:filter-patterns (get cache poshdb))))))


(defn poshdb->analyze-db [posh-tree poshdb]
  (let [conn-id (poshdb->conn-id poshdb)]
    {:db (poshdb->db posh-tree poshdb)
     :conn (conn-id->conn posh-tree conn-id)
     :schema (conn-id->schema posh-tree conn-id)
     :conn-id conn-id}))
