(ns mtgcoll.views.functions.statistics)

(defn owned-total
  [online? list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select coalesce(sum(cl.quantity), 0) as total
      from collection cl
      join lists l on cl.list_id = l.id
      where cl.online = ?
            and cl.list_id = ?
            and (l.is_public in (true, ?))"
     online? list-id public-only?]))

(defn distinct-owned-total
  [online? list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select count(distinct c.id)
      from cards c
      join collection cl on c.id = cl.card_id
      join lists l on cl.list_id = l.id
      where cl.quantity > 0
            and cl.online = ?
            and cl.list_id = ?
            and (l.is_public in (true, ?))"
     online? list-id public-only?]))

(defn owned-foil-total
  [online? list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select coalesce(sum(cl.quantity), 0) as total
      from collection cl
      join lists l on cl.list_id = l.id
      where cl.online = ?
            and cl.foil = true
            and cl.list_id = ?
            and (l.is_public in (true, ?))"
     online? list-id public-only?]))

(defn color-totals
  [online? list-id user-id]
  ;; TODO: i really dislike how this query is written ... try to clean this up at some point maybe?
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.color_identity like '%B%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as black,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.color_identity like '%U%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as blue,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.color_identity like '%G%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as green,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.color_identity like '%R%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as red,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.color_identity like '%W%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as white,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.color_identity = ''
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as colorless"
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?]))

(defn basic-type-totals
  [online? list-id user-id]
  ;; TODO: i really dislike how this query is written ... try to clean this up at some point maybe?
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.types like '%Artifact%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as artifacts,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.types like '%Creature%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as creatures,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.types like '%Enchantment%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as enchantments,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.types like '%Instant%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as instants,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.types like '%Land%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as lands,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.types like '%Planeswalker%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as planeswalkers,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.types like '%Tribal%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as tribals,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where c.types like '%Sorcery%'
                and cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as sorcerys"
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?
     online? list-id public-only?]))

(defn most-common-types
  [online? list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select *
      from (
          select sum(cl.quantity) as quantity, c.types
          from cards c
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
          group by c.types
      ) types_totals
      order by quantity desc
      limit 10"
     online? list-id public-only?]))

(defn total-sets-owned-from
  [online? list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select count(distinct c.set_code)
      from cards c
      join collection cl on c.id = cl.card_id
      join lists l on cl.list_id = l.id
      where cl.quantity > 0
            and cl.online = ?
            and cl.list_id = ?
            and (l.is_public in (true, ?))"
     online? list-id public-only?]))

(defn total-sets-owned-all-from
  [online? list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select count(*)
      from (
          select
          (
              select count(*)
              from cards
              where set_code = s.code
          ) as num_in_set,
          (
              select coalesce(sum(cl.quantity), 0)
              from cards c
              join collection cl on c.id = cl.card_id
              join lists l on cl.list_id = l.id
              where cl.quantity > 0
                    and cl.online = ?
                    and c.set_code = s.code
                    and cl.list_id = ?
                    and (l.is_public in (true, ?))
          ) as owned_in_set,
          s.code
          from sets s
      ) as set_owned_counts
      where set_owned_counts.num_in_set = set_owned_counts.owned_in_set"
     online? list-id public-only?]))

(defn most-owned-sets
  [online? list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select *
      from (
          select sum(cl.quantity) as quantity, c.set_code
          from cards c
          join sets s on c.set_code = s.code
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
          group by c.set_code
      ) sets_totals
      order by quantity desc
      limit 10"
     online? list-id public-only?]))

(defn most-copies-of-card
  [online? list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select quantity, c.id, c.name, c.set_code
      from (
          select sum(cl.quantity) as quantity, c2.id as card_id
          from cards c2
          join collection cl on c2.id = cl.card_id
          join lists l on cl.list_id = l.id
          where cl.quantity > 0
                and cl.online = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
          group by c2.id
      ) copies_of_cards
      join cards c on c.id = card_id
      order by quantity desc
      limit 10"
     online? list-id public-only?]))

(defn most-nonland-copies-of-card
  [online? list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select quantity, c.id, c.name, c.set_code
      from (
          select sum(cl.quantity) as quantity, c2.id as card_id
          from cards c2
          join collection cl on c2.id = cl.card_id
          join lists l on cl.list_id = l.id
          where cl.quantity > 0
                and cl.online = ?
                and c2.type not like '%Land%'
                and cl.list_id = ?
                and (l.is_public in (true, ?))
          group by c2.id
      ) copies_of_cards
      join cards c on c.id = card_id
      order by quantity desc
      limit 10"
     online? list-id public-only?]))

(defn total-price
  [online? price-source list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select cast(coalesce(sum(sub_total), 0) as decimal(10, 2)) as total
      from (
          select (cl.quantity * cp.price) as sub_total
          from cards c
          join card_prices cp on c.id = cp.card_id
          join collection cl on c.id = cl.card_id
          join lists l on cl.list_id = l.id
          where cl.quantity > 0
                and cl.online = ?
                and cp.online = ?
                and cp.source = ?
                and cl.list_id = ?
                and (l.is_public in (true, ?))
      ) as sub_totals"
     online? online? price-source list-id public-only?]))

(defn agg-price-stats
  [online? price-source list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select cast(coalesce(min(cp.price), 0) as decimal(10, 2)) as min_price,
             cast(coalesce(max(cp.price), 0) as decimal(10, 2)) as max_price,
             cast(coalesce(avg(cp.price), 0) as decimal(10, 2)) as avg_price,
             cast(coalesce(median(cp.price), 0) as decimal(10, 2)) as median_price
      from cards c
      join card_prices cp on c.id = cp.card_id
      join collection cl on c.id = cl.card_id
      join lists l on cl.list_id = l.id
      where cl.quantity > 0
            and cl.online = ?
            and cp.online = ?
            and cp.source = ?
            and cl.list_id = ?
            and (l.is_public in (true, ?))"
     online? online? price-source list-id public-only?]))

(defn most-valuable-cards
  [online? price-source list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select distinct c.id, c.name, c.set_code, cp.price
      from cards c
      join card_prices cp on c.id = cp.card_id
      join collection cl on c.id = cl.card_id
      join lists l on cl.list_id = l.id
      where cl.quantity > 0
            and cl.online = ?
            and cp.online = ?
            and cp.source = ?
            and cl.list_id = ?
            and (l.is_public in (true, ?))
      order by cp.price desc
      limit 10"
     online? online? price-source list-id public-only?]))

(defn num-cards-worth-over-1-dollar
  [online? price-source list-id user-id]
  (let [online?      (boolean online?)
        list-id      (int list-id)
        public-only? (nil? user-id)]
    ["select coalesce(sum(cl.quantity), 0) as count
      from cards c
      join card_prices cp on c.id = cp.card_id
      join collection cl on c.id = cl.card_id
      join lists l on cl.list_id = l.id
      where cl.quantity > 0
            and cl.online = ?
            and cp.online = ?
            and cp.source = ?
            and cp.price >= 1.0
            and cl.list_id = ?
            and (l.is_public in (true, ?))"
     online? online? price-source list-id public-only?]))

(defn card-rarity-totals
  [online? list-id user-id]
  (let [online? (boolean online?)
        list-id (int list-id)
        public-only? (nil? user-id)]
    ["select coalesce(sum(cl.quantity), 0) as total, c.rarity
      from cards c
      join collection cl on c.id = cl.card_id
      join lists l on cl.list_id = l.id
      where cl.quantity > 0
            and cl.online = ?
            and cl.list_id = ?
            and (l.is_public in (true, ?))
      group by c.rarity
      order by total desc"
     online? list-id public-only?]))