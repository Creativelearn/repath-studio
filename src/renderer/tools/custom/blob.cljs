(ns renderer.tools.custom.blob
  "Custom element for https://blobs.dev/"
  (:require
   ["blobs/v2" :as blobs]
   ["svgpath" :as svgpath]
   [clojure.core.matrix :as mat]
   [re-frame.core :as rf]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.attribute.length :as length]
   [renderer.attribute.views :as attr.v]
   [renderer.components :as comp]
   [renderer.element.handlers :as element.h]
   [renderer.tools.base :as tools]
   [renderer.tools.overlay :as overlay]
   [renderer.utils.element :as element]
   [renderer.utils.pointer :as pointer]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.units :as units]))

(derive :blob ::tools/custom)

(derive :size ::length/length)

(defmethod attr.hierarchy/form-element :extraPoints
  [k v disabled?]
  [attr.v/range-input k v {:min 0
                           :max 50
                           :step 1
                           :disabled disabled?} 0])

(defmethod attr.hierarchy/form-element :randomness
  [k v disabled?]
  [attr.v/range-input k v {:min 0
                           :max 50
                           :step 1
                           :disabled disabled?} 0])

(defmethod attr.hierarchy/form-element :seed
  [k v disabled?]
  (let [random-seed (rand-int 1000000)]
    [:<>
     [attr.v/form-input {:key k
                         :value v
                         :disabled? disabled?
                         :placeholder 0}]
     [:button.button.ml-px.inline-block.bg-primary.text-muted
      {:title "Generate random seed"
       :style {:flex "0 0 26px"
               :height "100%"}
       :on-click #(rf/dispatch [:element/set-attr k random-seed])}
      [comp/icon "refresh"]]]))

(defmethod attr.hierarchy/description :x
  []
  "Horizontal coordinate of the blob's center.")

(defmethod attr.hierarchy/description :y
  []
  "Vertical coordinate of the blob's center.")

(defmethod attr.hierarchy/description :seed
  []
  "A given seed will always produce the same blob.")

(defmethod attr.hierarchy/description :extraPoints
  []
  "The actual number of points will be `3 + extraPoints`.")

(defmethod attr.hierarchy/description :randomness
  []
  "Increases the amount of variation in point position.")

(defmethod attr.hierarchy/description :size
  []
  "The size of the bounding box.")

(defmethod tools/properties :blob
  []
  {:icon "blob"
   :description "Vector based blob."
   :url "https://blobs.dev/"
   :attrs [:x
           :y
           :seed
           :extraPoints
           :randomness
           :size
           :fill
           :stroke
           :stroke-width
           :opacity]})

(defmethod tools/drag-start :blob
  [{:keys [adjusted-pointer-offset
           active-document
           adjusted-pointer-pos] :as db}]
  (let [{:keys [stroke fill]} (get-in db [:documents active-document])
        [offset-x offset-y] adjusted-pointer-offset
        radius (mat/distance adjusted-pointer-pos adjusted-pointer-offset)]
    (element.h/set-temp db {:type :element
                            :tag :blob
                            :attrs {:x (- offset-x radius)
                                    :y (- offset-y radius)
                                    :seed (rand-int 1000000)
                                    :extraPoints 8
                                    :randomness 4
                                    :size (* radius 2)
                                    :fill fill
                                    :stroke stroke}})))

(defmethod tools/drag :blob
  [{:keys [adjusted-pointer-offset adjusted-pointer-pos] :as db}]
  (let [[offset-x offset-y] adjusted-pointer-offset
        radius (mat/distance adjusted-pointer-pos adjusted-pointer-offset)
        temp (-> (element.h/get-temp db)
                 (assoc-in [:attrs :x] (- offset-x radius))
                 (assoc-in [:attrs :y] (- offset-y radius))
                 (assoc-in [:attrs :size] (* radius 2)))]
    (element.h/set-temp db temp)))


(defmethod tools/translate :blob
  [element [x y]] (-> element
                      (attr.hierarchy/update-attr :x + x)
                      (attr.hierarchy/update-attr :y + y)))

(defmethod tools/scale :blob
  [el ratio pivot-point]
  (let [offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :size * ratio)
        (tools/translate offset))))

(defmethod tools/render :blob
  [{:keys [attrs children] :as element}]
  (let [child-elements @(rf/subscribe [:element/filter-visible children])
        pointer-handler #(pointer/event-handler % element)]
    [:path (merge {:d (tools/path element)
                   :on-pointer-up pointer-handler
                   :on-pointer-down pointer-handler
                   :on-pointer-move pointer-handler}
                  (select-keys attrs [:stroke
                                      :fill
                                      :stroke-width
                                      :id
                                      :class
                                      :opacity])) child-elements]))

(defmethod tools/translate :blob
  [element [x y]]
  (-> element
      (attr.hierarchy/update-attr :x + x)
      (attr.hierarchy/update-attr :y + y)))

(defmethod tools/position :blob
  [el [x y]]
  (let [dimensions (bounds/->dimensions (tools/bounds el))
        [cx cy] (mat/div dimensions 2)]
    (-> el
        (assoc-in [:attrs :x] (- x cx))
        (assoc-in [:attrs :y] (- y cy)))))

(defmethod tools/bounds :blob
  [{:keys [attrs]}]
  (let [{:keys [x y size]} attrs
        [x y size] (mapv units/unit->px [x y size])]
    [x y (+ x size) (+ y size)]))

(defmethod tools/centroid :blob
  [{{:keys [x y size]} :attrs}]
  (let [[x y size] (mapv units/unit->px [x y size])]
    (mat/add [x y] (/ size 2))))

(defmethod tools/path :blob
  [{:keys [attrs]}]
  (let [[x y] (mapv units/unit->px [(:x attrs) (:y attrs)])
        options (->> [:seed :extraPoints :randomness :size]
                     (select-keys attrs)
                     (reduce (fn [options [k v]] (assoc options k (int v))) {})
                     clj->js)]
    (-> blobs
        (.svgPath options)
        svgpath
        (.translate x y)
        .toString)))

(defmethod tools/edit :blob
  [element [x y] handler]
  (case handler
    :size
    (attr.hierarchy/update-attr element :size #(max 0 (+ % (min x y))))
    element))

(defmethod tools/render-edit :blob
  [{:keys [attrs key] :as el}]
  (let [{:keys [x y size]} attrs
        [x y size] (mapv units/unit->px [x y size])
        offset @(rf/subscribe [:element/el-offset el])
        [x1 y1] (cond->> [x y] (not (element/svg? el)) (mat/add offset))
        [x2 y2] (mat/add [x1 y1] size)]
    [:<>
     [overlay/line x1 y1 x2 y2]
     [overlay/square-handle
      {:type :handler
       :tag :edit
       :element key
       :x x2
       :y y2
       :key :size}]
     [overlay/times
      [x1 y1]]]))
