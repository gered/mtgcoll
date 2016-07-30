(ns mtgcoll.views.functions.lists)

(defn list-info
  [list-id user-id]
  (let [list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select name, notes, is_public, require_qualities
      from lists
      where id = ?
            and is_public in (true, ?)"
     list-id public-only?]))

(defn lists-list
  [user-id]
  (let [public-only? (nil? user-id)]
    ["select id, name, is_public, require_qualities
      from lists
      where id != 0
            and is_public in (true, ?)
      order by name"
     public-only?]))
