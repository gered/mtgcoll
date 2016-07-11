(ns mtgcoll.views.functions.cards
  (:require
    [clojure.string :as string]
    [honeysql.core :as hsql]
    [mtgcoll.common :refer [max-search-results]]))

(defn card-info
  [id]
  ["select c.id,
           c.name as card_name,
           c.set_code,
           s.name as set_name,
           c.mana_cost,
           c.colors,
           c.type,
           c.text,
           c.flavor,
           c.artist,
           c.power,
           c.toughness,
           c.loyalty
    from cards c
    join sets s on c.set_code = s.code
    where c.id = ?" id])

(defn full-card-info
  [id]
  ["select c.id,
           c.set_code,
           c.layout,
           c.name as card_name,
           c.mana_cost,
           c.converted_mana_cost,
           c.colors,
           c.color_identity,
           c.type,
           c.supertypes,
           c.types,
           c.subtypes,
           c.rarity,
           c.text,
           c.flavor,
           c.artist,
           c.number,
           c.power,
           c.toughness,
           c.loyalty,
           c.multiverseid,
           c.image_name,
           c.watermark,
           c.border,
           s.border as set_border,
           c.timeshifted,
           c.hand,
           c.life,
           c.reserved,
           c.release_date,
           c.starter,
           s.name as set_name,
           s.gatherer_code,
           s.old_code,
           s.magic_cards_info_code,
           s.release_date as set_release_date,
           s.type as set_type,
           s.block,
           s.online_only,
           c.paper_price,
           c.online_price,
           c.owned_count,
           c.owned_foil_count
    from cards c
    join sets s on c.set_code = s.code
    where c.id = ?" id])

(defn card-names
  [id]
  ["select name
    from card_names
    where card_id = ?
    order by name" id])

(defn card-variations
  [id]
  ["select c.id,
           c.set_code,
           c.name as card_name,
           s.name as set_name,
           c.release_date,
           s.release_date as set_release_date,
           s.type,
           s.online_only
    from card_variations cv
    join cards c on cv.variant_card_id = c.id
    join sets s on c.set_code = s.code
    where cv.card_id = ?
    order by s.release_date desc, c.name asc" id])

(defn card-printings
  [card-name]
  ["select c.id,
           c.set_code,
           c.name as card_name,
           s.name as set_name,
           c.release_date,
           s.release_date as set_release_date,
           s.type,
           s.online_only
    from cards c
    join sets s on c.set_code = s.code
    where c.name = ?" card-name])

(defn compare-fn
  [fields comparison value]
  (if (> (count fields) 1)
    (reduce
      (fn [v field]
        (conj v [comparison field value]))
      [:or]
      fields)
    [comparison (first fields) value]))

(defn text-comparison-fn
  [fields & [no-like?]]
  (fn [value comparison]
    (if no-like?
      (case comparison
        :=    (compare-fn fields := value))
      (case comparison
        :=    (compare-fn fields := value)
        :like (compare-fn fields :ilike (str "%" value "%"))))))

(defn numeric-comparison-fn
  [fields]
  (fn [value comparison]
    (case comparison
      := (compare-fn fields := value)
      :> (compare-fn fields :> value)
      :< (compare-fn fields :< value))))

(def search-field-where-clauses
  {:name             (text-comparison-fn [:name :normalized_name])
   :set-code         (text-comparison-fn [:set_code] true)
   :cmc              (numeric-comparison-fn [:converted_mana_cost])
   :colors           (fn [value comparison]
                       (let [colors (as-> value x
                                          (map string/trim x)
                                          (map string/capitalize x)
                                          (sort x))]
                         (case comparison
                           := [:= :colors (string/join "," colors)]
                           :like [:ilike :colors (str "%" (string/join "%" colors) "%")])))
   :color-identity   (fn [value comparison]
                       (let [colors (as-> value x
                                          (map string/trim x)
                                          (map string/upper-case x)
                                          (sort x))]
                         (case comparison
                           := [:= :color_identity (string/join "," colors)]
                           :like [:like :color_identity (str "%" (string/join "%" colors) "%")])))
   :type             (text-comparison-fn [:type])
   :rarity           (text-comparison-fn [:rarity] true)
   :text             (text-comparison-fn [:text])
   :artist           (text-comparison-fn [:artist])
   :number           (text-comparison-fn [:number] true)
   :power            (text-comparison-fn [:power] true)
   :toughness        (text-comparison-fn [:toughness] true)
   :owned-count      (numeric-comparison-fn [:owned_count])
   :owned-foil-count (numeric-comparison-fn [:owned_foil_count])
   :paper-price      (numeric-comparison-fn [:paper_price])
   :online-price     (numeric-comparison-fn [:online_price])
   :owned?           (fn [value comparison]
                       (assert (= := comparison))
                       (case value
                         true [:> :owned_count 0]
                         false [:= :owned_count 0]))
   :owned-foil?      (fn [value comparison]
                       (assert (= := comparison))
                       (case value
                         true [:> :owned_foil_count 0]
                         false [:= :owned_foil_count 0]))})

(defn- filter->hsql-where-criteria
  [{:keys [field value comparison]}]
  (if-let [f (get search-field-where-clauses field)]
    (f value comparison)))

(defn- base-card-search-query
  [hsql-filters & [order-by]]
  (let [q {:from [[(merge
                     {:select [:c.id
                               :c.name
                               :c.normalized_name
                               :c.set_code
                               [:s.name :set_name]
                               :c.mana_cost
                               :c.converted_mana_cost
                               :c.colors
                               :c.color_identity
                               :c.type
                               :c.rarity
                               :c.text
                               :c.flavor
                               :c.artist
                               :c.number
                               :c.power
                               :c.toughness
                               :c.loyalty
                               :c.paper_price
                               :c.online_price
                               :c.owned_count
                               :c.owned_foil_count]
                      :from   [[:cards :c]]
                      :join   [[:sets :s] [:= :c.set_code :s.code]]}
                     (if order-by
                       {:order-by [order-by [:c.id]]}))
                   :cards_list]]}]
    (if (seq hsql-filters)
      (assoc q :where (concat [:and] hsql-filters))
      q)))

(defn cards
  [filters & [sort-by ascending? page-num page-size]]
  (let [sort-by      (case sort-by
                       :name :name
                       :set :set_code
                       :mana-cost :converted_mana_cost
                       :type :type
                       :rarity :rarity
                       :paper-price :paper_price
                       :online-price :online_price
                       :inventory :owned_count
                       :name)
        sort-dir     (if ascending? :asc :desc)
        nulls        (if (some #{sort-by} #{:converted_mana_cost :paper_price :online_price :owned_count})
                       (if-not ascending? :nulls-last :nulls-first))
        page-size    (or page-size 25)
        page-size    (if (> page-size 200) 200 page-size)
        page-num     (or page-num 0)
        page-num     (if (> (* page-num page-size) max-search-results)
                       (dec (int (Math/floor (/ max-search-results page-size))))
                       page-num)
        hsql-filters (mapv filter->hsql-where-criteria filters)
        q            (base-card-search-query
                       hsql-filters
                       (if nulls
                         [sort-by sort-dir nulls]
                         [sort-by sort-dir]))
        q            (assoc q :select [:*]
                              :limit page-size
                              :offset (* page-num page-size))]
    (hsql/format q)))

(defn count-of-cards
  [filters]
  (let [hsql-filters (mapv filter->hsql-where-criteria filters)
        q            (base-card-search-query hsql-filters)
        q            (assoc q :select [:%count.*])]
    (hsql/format q)))

#_(cards [{:field :name :value "z" :comparison :like}] :name true 10000 20)