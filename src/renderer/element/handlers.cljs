(ns renderer.element.handlers
  (:require
   ["paper" :refer [Path]]
   [clojure.core.matrix :as mat]
   [clojure.set :as set]
   [renderer.attribute.hierarchy :as hierarchy]
   [renderer.tools.base :as tools]
   [renderer.tools.path :as path]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.map :as map]
   [renderer.utils.uuid :as uuid]
   [renderer.utils.vec :as vec]
   [renderer.utils.spec :as spec]))

(defn path
  ([db]
   [:documents (:active-document db) :elements])
  ([db el-k]
   (conj (path db) el-k)))

(defn update-el
  [db el f & more]
  (apply update-in db (conj (path db) (:key el)) f more))

(defn elements
  ([db]
   (get-in db (path db)))
  ([db keys]
   (vals (select-keys (elements db) (vec keys)))))

(defn element
  [db k]
  (k (elements db)))

(defn active-page
  [db]
  (->> (get-in db [:documents (:active-document db) :active-page])
       (element db)))

(defn selected
  [db]
  (filter :selected? (vals (elements db))))

(defn selected-keys
  [db]
  (into #{} (map :key (selected db))))

(defn parent
  ([db]
   (let [selected (selected db)]
     (cond
       (empty? selected)
       (active-page db)

       (let [parents (into #{} (map :parent selected))]
         (and (first parents)
              (empty? (rest parents))
              (= (count selected)
                 (count (:children (element db (first parents)))))))
       (or (parent db (parent db (first selected)))
           (active-page db))

       (= (count selected) 1)
       (or (parent db (first selected))
           (active-page db))

       :else
       (active-page db))))
  ([db el]
   (when-let [parent (:parent el)]
     (element db parent))))

(defn siblings
  ([db]
   (:children (parent db)))
  ([db el]
   (:children (parent db el))))

(defn pages
  [db]
  (vals (select-keys (elements db) (-> (elements db) :canvas :children))))

(defn page?
  [el]
  (= :page (:tag el)))

(defn ancestor-keys
  [db el]
  (loop [parent-key (:parent el)
         parent-keys #{}]
    (if parent-key
      (recur
       (:parent (element db parent-key))
       (conj parent-keys parent-key))
      parent-keys)))

(defn descendant-keys
  ([db]
   (reduce #(set/union %1 (descendant-keys db %2)) #{} (selected db)))
  ([db el]
   (loop [children (set (:children el))
          child-keys #{}]
     (if (seq children)
       (recur
        (reduce #(set/union %1 (:children (element db %2))) #{} children)
        (set/union child-keys children))
       child-keys))))

(defn top-selected-keys
  [db]
  (set/difference (selected-keys db) (descendant-keys db)))

(defn top-selected
  [db]
  (elements db (top-selected-keys db)))

(defn clear-temp
  [db]
  (update-in db [:documents (:active-document db)] dissoc :temp-element))

(defn set-temp
  [db el]
  (assoc-in db [:documents (:active-document db) :temp-element] el))

(defn get-temp
  [db]
  (get-in db [:documents (:active-document db) :temp-element]))

(defn page
  [db el]
  (if (or (not (:parent el)) (= (:tag el) :canvas))
    (active-page db)
    (if (page? el)
      el
      (recur db (parent db el)))))

(defn set-active-page
  [db key]
  (assoc-in db [:documents (:active-document db) :active-page] key))

(defn next-active-page
  [db]
  (set-active-page db (last (-> (elements db) :canvas :children))))

(defn update-property
  [db el-k k f & more]
  (apply update-in db (conj (path db) el-k k) f more))

(defn toggle-property
  [db el-k k]
  (update-property db el-k k not))

(defn set-property
  ([db k v]
   (reduce #(set-property %1 %2 k v) db (selected-keys db)))
  ([db el-k k v]
   (assoc-in db (conj (path db) el-k k) v)))

(defn remove-attribute
  ([db k]
   (reduce #(remove-attribute %1 %2 k) db (selected-keys db)))
  ([db el-k k]
   (cond-> db
     (not (:locked? (element db el-k)))
     (update-property el-k :attrs dissoc k))))

(defn supports-attr?
  [el k]
  (-> el tools/attributes k))

(defn set-attribute
  ([db k v]
   (reduce #(set-attribute %1 %2 k v) db (selected-keys db)))
  ([db el-k k v]
   (let [attr-path (conj (path db) el-k :attrs k)
         el (element db el-k)]
     (if (and (not (:locked? el)) (supports-attr? el k))
       (if (empty? v)
         (remove-attribute db el-k k)
         (assoc-in db attr-path v))
       db))))

(defn update-attribute
  ([db k f]
   (reduce #(update-attribute %1 %2 k f) db (selected db)))
  ([db el k f]
   (cond-> db
     (and (not (:locked? el)) (supports-attr? el k))
     (update-el el hierarchy/update-attr k f))))

(defn deselect
  ([db]
   (reduce deselect db (keys (elements db))))
  ([db key]
   (set-property db key :selected? false)))

(defn select
  ([db key]
   (set-property db key :selected? true))
  ([db key multi?]
   (if-let [el (element db key)]
     (if-not multi?
       (-> db
           deselect
           (select key)
           (set-active-page (:key (page db el))))
       (toggle-property db key :selected?))
     (deselect db))))

(defn select-all
  [db]
  (reduce select (deselect db) (siblings db)))

(defn selected-tags
  [db]
  (reduce #(conj %1 (:tag %2)) #{} (selected db)))

(defn select-same-tags
  [db]
  (let [selected-tags (selected-tags db)]
    (reduce (fn [db {:keys [key tag]}]
              (cond-> db
                (contains? selected-tags tag)
                (select key))) (deselect db) (vals (elements db)))))

(defn invert-selection
  [db]
  (reduce (fn [db {:keys [key tag]}]
            (cond-> db
              (not (contains? #{:page :canvas} tag))
              (update-property key :selected? not)))
          db
          (vals (elements db))))

(defn hover
  [db k]
  (update-in db [:documents (:active-document db) :hovered-keys] conj k))

(defn ignore
  [db k]
  (update-in db [:documents (:active-document db) :ignored-keys] conj k))

(defn clear-hovered
  [db]
  (assoc-in db [:documents (:active-document db) :hovered-keys] #{}))

(defn clear-ignored
  [db]
  (assoc-in db [:documents (:active-document db) :ignored-keys] #{}))

(defmulti intersects-with-bounds? (fn [element _] (:tag element)))

(defmethod intersects-with-bounds? :default [])

(defn lock
  ([db]
   (reduce lock db (selected-keys db)))
  ([db el-k]
   (set-property db el-k :locked? true)))

(defn unlock
  ([db]
   (reduce unlock db (selected-keys db)))
  ([db el-k]
   (set-property db el-k :locked? false)))

(defn copy
  [db]
  (assoc db :copied-elements (selected db)))

(defn delete
  ([db]
   (reduce delete db (selected-keys db)))
  ([db k]
   (let [el (element db k)
         ;; OPTIMIZE: No need to recur to delete all children
         db (reduce delete db (:children el))]
     (cond-> db
       :always
       (assoc-in
        (conj (path db) (:parent el) :children)
        (vec (remove #{k} (siblings db el))))

       (page? el)
       (next-active-page)

       :always
       (update-in (path db) dissoc k)))))

(defn update-index
  [db el f & more]
  (let [siblings (siblings db el)
        index (.indexOf siblings (:key el))
        new-index (f index)]
    (cond-> db
      (<= 0 new-index (-> siblings count dec))
      (update-property (:parent el) :children vec/move index (apply f index more)))))

(defn raise
  ([db]
   (reduce raise db (selected db)))
  ([db el]
   (update-index db el inc)))

(defn lower
  ([db]
   (reduce lower db (selected db)))
  ([db el]
   (update-index db el dec)))

(defn lower-to-bottom
  ([db]
   (reduce lower-to-bottom db (selected db)))
  ([db el]
   (update-index db el (fn [_] 0))))

(defn raise-to-top
  ([db]
   (reduce raise-to-top db (selected db)))
  ([db el]
   (update-index db el #(-> (siblings db el) count dec))))

(defn translate
  ([db offset]
   (reduce #(translate %1 %2 offset) db (top-selected db)))
  ([db el offset]
   (cond-> db
     (not (:locked? el))
     (update-el el tools/translate offset))))

(defn bounds
  ([db]
   (tools/elements-bounds (elements db) (selected db)))
  ([db elements]
   (tools/elements-bounds elements (selected db))))

(defn scale
  ([db ratio pivot-point]
   (reduce #(scale %1 %2 ratio pivot-point) db (selected db)))
  ([db el ratio pivot-point]
   (cond-> db
     (not (:locked? el))
     (update-el el
                tools/scale
                ratio
                (let [[x1 y1] (tools/adjusted-bounds el (elements db))]
                  (mat/sub pivot-point [x1 y1]))))))

(defn align
  ([db direction]
   (reduce #(align %1 %2 direction) db (selected db)))
  ([db el direction]
   (let [bounds (tools/adjusted-bounds el (elements db))
         center (bounds/center bounds)
         parent-bounds (tools/adjusted-bounds (parent db el) (elements db))
         parent-center (bounds/center parent-bounds)
         [cx cy] (mat/sub parent-center center)
         [x1 y1 x2 y2] (mat/sub parent-bounds bounds)]
     (translate db el (case direction
                        :top [0 y1]
                        :center-vertical [0 cy]
                        :bottom [0 y2]
                        :left [x1 0]
                        :center-horizontal [cx 0]
                        :right [x2 0])))))

(defn ->path
  ([db]
   (reduce ->path db (selected db)))
  ([db el]
   (cond-> db
     (not (:locked? el)) (update-el el tools/->path))))

(defn stroke->path
  ([db]
   (reduce stroke->path db (selected db)))
  ([db el]
   (cond-> db
     (not (:locked? el)) (update-el el tools/stroke->path))))

(def default-props
  {:type :element
   :visible? true
   :selected? true
   :children []})

(defn create-element
  [db el]
  (let [key (uuid/generate)
        parent (if (page? el) :canvas (-> db active-page :key))
        el (map/deep-merge el default-props {:key key :parent parent})]
    (cond-> db
      :always
      (-> (assoc-in (conj (path db) key) el)
          (update-property (:parent el) :children #(vec (conj % key))))

      (not= (:tool db) :select)
      (tools/set-tool :select)

      (page? el)
      (set-active-page key))))

(defn create
  ([db]
   (-> db
       (create (get-temp db))
       clear-temp))
  ([db & elements]
   ;; TODO: Handle child elements
   (let [[x1 y1 _ _] (tools/bounds (active-page db))]
     (reduce #(cond-> (create-element %1 %2)
                (not (page? %2)) (translate [(- x1) (- y1)]))
             (deselect db) elements))))

(defn bool
  [path-a path-b operation]
  (case operation
    :unite (.unite path-a path-b)
    :intersect (.intersect path-a path-b)
    :subtract (.subtract path-a path-b)
    :exclude (.exclude path-a path-b)
    :divide (.divide path-a path-b)))

(defn bool-operation
  [db operation]
  ;; TODO: Sort elements by visibily index
  (let [selected-elements (selected db)
        attrs (-> selected-elements first tools/->path :attrs)
        new-path (reduce (fn [path element]
                           (let [path-a (Path. path)
                                 path-b (-> element tools/->path :attrs :d Path.)]
                             (-> (bool path-a path-b operation)
                                 (.exportSVG)
                                 (.getAttribute "d"))))
                         (:d attrs)
                         (rest selected-elements))]
    (-> db
        delete
        (create {:type :element
                 :tag :path
                 :attrs (merge attrs {:d new-path})}))))

(defn paste-in-place
  ([db]
   (reduce paste-in-place (deselect db) (:copied-elements db)))
  ([db el]
   (create-element db (assoc el :parent (-> db active-page :key)))))

(defn paste
  [db]
  (let [db (paste-in-place db)
        bounds (bounds db)
        [x1 y1] bounds
        [width height] (bounds/->dimensions bounds)
        [x y] (:adjusted-pointer-pos db)]
    (translate db [(- x (+ x1 (/ width 2)))
                   (- y (+ y1 (/ height 2)))])))

(defn duplicate-in-place
  ([db]
   (reduce duplicate-in-place (deselect db) (selected db)))
  ([db el]
   (create-element db el)))

(defn duplicate
  [db offset]
  (-> db
      duplicate-in-place
      (translate offset)))

(defn animate
  ([db tag attrs]
   (reduce #(animate %1 %2 tag attrs) (deselect db) (selected db)))
  ([db el tag attrs]
   (create-element db {:tag tag :attrs attrs :parent (:key el)})))

(defn paste-styles
  ([db]
   (reduce paste-styles db (selected db)))
  ([{copied-elements :copied-elements :as db} el]
   ;; TODO: Merge attributes from multiple selected elements.
   (if (= 1 (count copied-elements))
     (let [attrs (:attrs (first copied-elements))
           style-attrs (disj spec/presentation-attrs :transform)]
       (reduce (fn [db attr]
                 (cond-> db
                   (-> attrs attr)
                   (update-attribute el attr #(if % (-> attrs attr) disj))))
               db style-attrs)) db)))

(defn set-parent
  ([db parent-key]
   (reduce #(set-parent %1 %2 parent-key) db (selected-keys db)))
  ([db element-key parent-key]
   (cond-> db
     (not= element-key parent-key)
     (-> (update-property (:parent (element db element-key))
                          :children
                          #(vec (remove #{element-key} %)))
         (update-property parent-key :children conj element-key)
         (set-property element-key :parent parent-key)))))

(defn set-parent-at-index
  [db element-key parent-key index]
  (let [siblings (:children (element db parent-key))
        last-index (-> siblings count)]
    (-> db
        (set-parent element-key parent-key)
        (update-property parent-key :children vec/move last-index index))))

(defn group
  [db]
  (reduce (fn [db key] (set-parent db key (first (selected-keys db))))
          (-> (deselect db)
              (create-element {:tag :g :parent (:key (active-page db))}))
          (selected-keys db)))

(defn inherit-attrs
  [db source-el target-el-k]
  (reduce
   (fn [db attr]
     (let [source-attr (-> source-el :attrs attr)]
       (cond-> db
         source-attr
         (update-attribute (element db target-el-k)
                           attr
                           (fn [v]
                             (if (empty? (str v))
                               source-attr
                               v)))))) db spec/presentation-attrs))

(defn ungroup
  ([db]
   (reduce ungroup db (selected db)))
  ([db el]
   (cond-> db
     (and (not (:locked? el))
          (= (:tag el) :g))
     (as-> db db
       (let [siblings (siblings db el)
             index (.indexOf siblings (:key el))]
         (reduce
          (fn [db el-k]
            (-> db
                (set-parent-at-index el-k (:parent el) index)
                ;; Group attributes are inherited by its children, 
                ;; so we need to maintain the presentation attrs.
                (inherit-attrs el el-k)))
          db (reverse (:children el))))
       (delete db (:key el))))))

(defn manipulate-path
  ([db action]
   (reduce #(manipulate-path %1 %2 action) db (selected db)))
  ([db el action]
   (cond-> db
     (and (not (:locked? el))
          (= (:tag el) :path))
     (update-in (path db (:key el)) path/manipulate action))))
