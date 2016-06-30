(ns mtgcoll.views.functions.sets)

(defn set-info
  [code]
  ["select s.code,
           s.name,
           s.release_date,
           s.border,
           s.type,
           s.block,
           s.online_only,
           (
               select count(*)
               from cards c
               where c.set_code = s.code
           ) as card_count,
           (
               select count(*)
               from collection cl
               join cards c on cl.card_id = c.id
               where c.set_code = s.code and cl.quantity > 0
           ) as owned_count
    from sets s
    where s.code = ?" code])

(defn simple-sets-list
  []
  ["select s.code, s.name
    from sets s
    order by s.name"])

(defn sets-list
  []
  ["select s.code,
           s.name,
           s.release_date,
           s.border,
           s.type,
           s.block,
           s.online_only,
           (
               select count(*)
               from cards c
               where c.set_code = s.code
           ) as card_count,
           (
               select count(*)
               from collection cl
               join cards c on cl.card_id = c.id
               where c.set_code = s.code and cl.quantity > 0
           ) as owned_count
    from sets s"])
