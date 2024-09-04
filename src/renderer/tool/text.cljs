(ns renderer.tool.text
  (:require
   [clojure.core.matrix :as mat]
   [clojure.string :as str]
   [re-frame.core :as rf]
   [renderer.app.events :as-alias app.e]
   [renderer.app.handlers :as app.h]
   [renderer.attribute.hierarchy :as attr.hierarchy]
   [renderer.element.events :as-alias element.e]
   [renderer.element.handlers :as element.h]
   [renderer.tool.hierarchy :as tool.hierarchy]
   [renderer.utils.bounds :as bounds]
   [renderer.utils.element :as element]
   [renderer.utils.units :as units]))

(derive :text ::tool.hierarchy/shape)

(defmethod tool.hierarchy/properties :text
  []
  {:icon "text"
   :description "The SVG <text> element draws a graphics element consisting
                 of text. It's possible to apply a gradient, pattern,
                 clipping path, mask, or filter to <text>, like any other SVG
                 graphics element."
   :attrs [:font-family
           :font-size
           :font-weight
           :font-style
           :fill
           :stroke
           :stroke-width
           :opacity]})

(defmethod tool.hierarchy/activate :text
  [db]
  (-> db
      (assoc :cursor "text")
      (app.h/set-message "Click to enter your text.")))

(defmethod tool.hierarchy/pointer-up :text
  [{:keys [adjusted-pointer-offset] :as db} _e]
  (let [[offset-x offset-y] adjusted-pointer-offset
        attrs {:x offset-x
               :y offset-y}]
    (-> db
        (element.h/deselect)
        (element.h/add {:type :element
                        :tag :text
                        :attrs attrs})
        (app.h/set-tool :edit)
        (app.h/set-state :create))))

(defmethod tool.hierarchy/drag-end :text
  [db e]
  (tool.hierarchy/pointer-up db e))

(defmethod tool.hierarchy/translate :text
  [el [x y]]
  (-> el
      (attr.hierarchy/update-attr :x + x)
      (attr.hierarchy/update-attr :y + y)))

(defmethod tool.hierarchy/scale :text
  [el ratio pivot-point]
  (let [offset (mat/sub pivot-point (mat/mul pivot-point ratio))
        ratio (apply min ratio)]
    (-> el
        (attr.hierarchy/update-attr :font-size * ratio)
        (tool.hierarchy/translate offset))))

(defn get-text
  [e]
  (str/replace (.. e -target -value) " " "\u00a0")) ; REVIEW

(defn set-text-and-select-element
  [e id]
  (let [s (get-text e)]
    (rf/dispatch (if (empty? s)
                   [::element.e/delete-by-id id]
                   [::element.e/set-prop id :content s]))
    (rf/dispatch [::app.e/set-tool :select])))

(defn key-down-handler
  [e id]
  (.stopPropagation e)
  (if (contains? #{"Enter" "Escape"} (.-code e))
    (set-text-and-select-element e id)
    (.requestAnimationFrame
     js/window
     #(rf/dispatch-sync [::element.e/preview-prop id :content (get-text e)]))))

(defmethod tool.hierarchy/render-edit :text
  [{:keys [attrs id content] :as el}]
  (let [offset (element/offset el)
        el-bounds (tool.hierarchy/bounds el)
        [x y] (mat/add (take 2 el-bounds) offset)
        [width height] (bounds/->dimensions el-bounds)
        {:keys [fill font-family font-size font-weight]} attrs]
    [:foreignObject {:x x
                     :y y
                     :width (+ width 20)
                     :height height}
     [:input
      {:key id
       :default-value content
       :auto-focus true
       :on-focus #(.. % -target select)
       :on-pointer-down #(.stopPropagation %)
       :on-pointer-up #(.stopPropagation %)
       :on-blur #(set-text-and-select-element % id)
       :on-key-down #(key-down-handler % id)
       :style {:color "transparent"
               :caret-color (or fill "black")
               :display "block"
               :width (+ width 15)
               :height height
               :padding 0
               :border 0
               :outline "none"
               :background "transparent"
               :font-family (if (empty? font-family) "inherit" font-family)
               :font-size (if (empty? font-size)
                            "inherit"
                            (str (units/unit->px font-size) "px"))
               :font-weight (if (empty? font-weight) "inherit" font-weight)}}]]))

(defmethod tool.hierarchy/path :text
  [{:keys [attrs content]}]
  (let [font-descriptor #js {:family (:font-family attrs)
                             :weight (js/parseInt (:font-weight attrs))
                             :italic (= (:font-style attrs) "italic")}]
    (.textToPath
     js/window.api
     content
     #js {:font-url (.-path (.findFont js/window.api font-descriptor))
          :x (js/parseFloat (:x attrs))
          :y (js/parseFloat (:y attrs))
          :font-size (js/parseFloat (or (:font-size attrs) 16))}))) ; FIXME
