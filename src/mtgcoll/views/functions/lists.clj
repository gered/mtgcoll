(ns mtgcoll.views.functions.lists)

(defn list-info
  [list-id user-id]
  (let [list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select name, notes, is_public, require_qualities, created_at
      from lists
      where id = ?
            and is_public in (true, ?)"
     list-id public-only?]))

(defn list-settings
  [list-id user-id]
  (let [list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select is_public, require_qualities
      from lists
      where id = ?
            and is_public in (true, ?)"
     list-id public-only?]))

(defn lists-list
  [user-id]
  (let [public-only? (nil? user-id)]
    ["select l.id,
             l.name,
             l.is_public,
             l.require_qualities,
             l.created_at,
             (
                 select coalesce(sum(lc.quantity), 0)
                 from lists_card_quantities lc
                 where lc.list_id = l.id
             ) as num_cards
      from lists l
      where l.id != 0
            and l.is_public in (true, ?)
      order by l.name"
     public-only?]))

(defn lists-basic-list
  [user-id]
  (let [public-only? (nil? user-id)]
    ["select id, name
      from lists
      where id != 0
            and is_public in (true, ?)
      order by name"
     public-only?]))
