(ns mtgcoll.views.functions.statistics)

(defn owned-total
  [online?]
  ["select sum(quantity)
    from collection
    where online = ?"
   (boolean online?)])

(defn distinct-owned-total
  [online?]
  ["select count(*)
    from cards c
    join collection cl on c.id = cl.card_id
    where cl.quantity > 0 and cl.online = ?"
   (boolean online?)])

(defn color-totals
  [online?]
  (let [online? (boolean online?)]
    ["select
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.color_identity like '%B%' and cl.quantity > 0 and cl.online = ?
      ) as black,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.color_identity like '%U%' and cl.quantity > 0 and cl.online = ?
      ) as blue,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.color_identity like '%G%' and cl.quantity > 0 and cl.online = ?
      ) as green,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.color_identity like '%R%' and cl.quantity > 0 and cl.online = ?
      ) as red,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.color_identity like '%W%' and cl.quantity > 0 and cl.online = ?
      ) as white,
      (
          select sum(cl.quantity)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.color_identity = '' and cl.quantity > 0 and cl.online = ?
      ) as colorless"
     online? online? online? online? online? online?]))

(defn basic-type-totals
  [online?]
  (let [online? (boolean online?)]
    ["select
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.types like '%Artifact%' and cl.quantity > 0 and cl.online = ?
      ) as artifacts,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.types like '%Creature%' and cl.quantity > 0 and cl.online = ?
      ) as creatures,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.types like '%Enchantment%' and cl.quantity > 0 and cl.online = ?
      ) as enchantments,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.types like '%Instant%' and cl.quantity > 0 and cl.online = ?
      ) as instants,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.types like '%Land%' and cl.quantity > 0 and cl.online = ?
      ) as lands,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.types like '%Planeswalker%' and cl.quantity > 0 and cl.online = ?
      ) as planeswalkers,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.types like '%Tribal%' and cl.quantity > 0 and cl.online = ?
      ) as tribals,
      (
          select coalesce(sum(cl.quantity), 0)
          from cards c
          join collection cl on c.id = cl.card_id
          where c.types like '%Sorcery%' and cl.quantity > 0 and cl.online = ?
      ) as sorcerys"
     online? online? online? online? online? online? online? online?]))

(defn most-common-types
  [online?]
  ["select *
    from (
        select sum(cl.quantity) as quantity, c.types
        from cards c
        join collection cl on c.id = cl.card_id
        where cl.quantity > 0 and cl.online = ?
        group by c.types
    ) types_totals
    order by quantity desc
    limit 10"
   (boolean online?)])

(defn total-sets-owned-from
  [online?]
  ["select count(distinct c.set_code)
    from cards c
    join collection cl on c.id = cl.card_id
    where cl.quantity > 0 and cl.online = ?"
   (boolean online?)])

(defn total-sets-owned-all-from
  [online?]
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
            where cl.quantity > 0 and cl.online = ? and c.set_code = s.code
        ) as owned_in_set,
        s.code
        from sets s
    ) as set_owned_counts
    where set_owned_counts.num_in_set = set_owned_counts.owned_in_set"
   (boolean online?)])

(defn most-owned-sets
  [online?]
  ["select *
    from (
        select sum(cl.quantity) as quantity, c.set_code
        from cards c
        join sets s on c.set_code = s.code
        join collection cl on c.id = cl.card_id
        where cl.quantity > 0 and cl.online = ?
        group by c.set_code
    ) sets_totals
    order by quantity desc
    limit 10"
   (boolean online?)])

(defn most-copies-of-card
  [online?]
  ["select quantity, c.id, c.name, c.set_code
    from (
        select sum(cl.quantity) as quantity, c2.id as card_id
        from cards c2
        join collection cl on c2.id = cl.card_id
        where cl.quantity > 0 and cl.online = ?
        group by c2.id
    ) copies_of_cards
    join cards c on c.id = card_id
    order by quantity desc
    limit 10"
   (boolean online?)])

(defn most-nonland-copies-of-card
  [online?]
  ["select quantity, c.id, c.name, c.set_code
    from (
        select sum(cl.quantity) as quantity, c2.id as card_id
        from cards c2
        join collection cl on c2.id = cl.card_id
        where cl.quantity > 0 and cl.online = ? and c2.type not like '%Land%'
        group by c2.id
    ) copies_of_cards
    join cards c on c.id = card_id
    order by quantity desc
    limit 10"
   (boolean online?)])

(defn total-price
  [online? price-source]
  (let [online? (boolean online?)]
    ["select cast(coalesce(sum(sub_total), 0) as decimal(10, 2)) as total
      from (
          select (cl.quantity * cp.price) as sub_total
          from cards c
          join card_prices cp on c.id = cp.card_id
          join collection cl on c.id = cl.card_id
          where cl.quantity > 0 and cl.online = ? and cp.online = ? and cp.source = ?
      ) as sub_totals"
     online? online? price-source]))

(defn agg-price-stats
  [online? price-source]
  (let [online? (boolean online?)]
    ["select cast(coalesce(min(cp.price), 0) as decimal(10, 2)) as min_price,
             cast(coalesce(max(cp.price), 0) as decimal(10, 2)) as max_price,
             cast(coalesce(avg(cp.price), 0) as decimal(10, 2)) as avg_price,
             cast(coalesce(median(cp.price), 0) as decimal(10, 2)) as median_price
      from cards c
      join card_prices cp on c.id = cp.card_id
      join collection cl on c.id = cl.card_id
      where cl.quantity > 0 and cl.online = ? and cp.online = ? and cp.source = ?"
     online? online? price-source]))

(defn most-valuable-cards
  [online? price-source]
  (let [online? (boolean online?)]
    ["select c.id, c.name, c.set_code, cp.price
      from cards c
      join card_prices cp on c.id = cp.card_id
      join collection cl on c.id = cl.card_id
      where cl.quantity > 0 and cl.online = ? and cp.online = ? and cp.source = ?
      order by cp.price desc
      limit 10"
     online? online? price-source]))

(defn num-cards-worth-over-1-dollar
  [online? price-source]
  (let [online? (boolean online?)]
    ["select coalesce(sum(cl.quantity), 0) as count
      from cards c
      join card_prices cp on c.id = cp.card_id
      join collection cl on c.id = cl.card_id
      where cl.quantity > 0 and cl.online = ? and cp.online = ? and cp.source = ? and cp.price >= 1.0"
     online? online? price-source]))

(defn card-rarity-totals
  [online?]
  ["select coalesce(sum(cl.quantity), 0) as total, c.rarity
    from cards c
    join collection cl on c.id = cl.card_id
    where cl.quantity > 0 and cl.online = ?
    group by c.rarity
    order by total desc"
   (boolean online?)])