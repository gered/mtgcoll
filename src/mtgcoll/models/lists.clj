(ns mtgcoll.models.lists
  (:require
    [views.sql.core :refer [vexec! with-view-transaction]]
    [mtgcoll.db :refer [db]]
    [mtgcoll.views.core :refer [view-system]]))

(defn add-list!
  [name public? requires-qualities?]
  (let [result (vexec! view-system db
                       ["insert into lists
                         (name, is_public, require_qualities)
                         values
                         (?, ?, ?)
                         returning id"
                        (str name) (boolean public?) (boolean requires-qualities?)])]
    (->> result first :id)))

(defn remove-list!
  [list-id]
  (if (= 0 list-id)
    (throw (Exception. "Cannot remove the 'Owned' list."))
    (vexec! view-system db
            ["delete from lists
            where id = ?"
             (int list-id)])))

(defn update-list-name!
  [list-id name]
  (if (= 0 list-id)
    (throw (Exception. "Cannot change the name of the 'Owned' list."))
    (vexec! view-system db
            ["update lists
            set name = ?
            where id = ?"
             (str name) (int list-id)])))

(defn update-list-note!
  [list-id note]
  (vexec! view-system db
          ["update lists
            set notes = ?
            where id = ?"
           (str note) (int list-id)]))

(defn update-list-visibility!
  [list-id public?]
  (if (= 0 list-id)
    (throw (Exception. "Cannot change the visibility of the 'Owned' list."))
    (vexec! view-system db
            ["update lists
            set is_public = ?
            where id = ?"
             (boolean public?) (int list-id)])))
